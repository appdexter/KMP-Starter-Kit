@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.kotlinfoundation.koko.presentation.screens.profile

import com.kotlinfoundation.koko.data.source.preferences.FakeUserPreferences
import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.domain.model.User
import com.kotlinfoundation.koko.util.AppUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProfileViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()
    private lateinit var userPreferences: FakeUserPreferences
    private lateinit var userFlow: MutableSharedFlow<Result<User>>
    private lateinit var fakeAppUtil: FakeAppUtil
    private lateinit var viewModel: ProfileViewModel

    private class FakeAppUtil(private val version: String = "2.3.4") : AppUtil {
        override fun getAppName(): String = "Koko"
        override fun shareApp() {}
        override fun openFeedbackMail() {}
        override fun getAppVersionInfo(): String = version
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        userPreferences = FakeUserPreferences()
        userFlow = MutableSharedFlow(replay = 1)
        userFlow.tryEmit(
            Result.success(User(id = "user_123", displayName = "Test User", email = "test@example.com")),
        )
        fakeAppUtil = FakeAppUtil("2.3.4")
        viewModel = ProfileViewModel(
            userPreferences = userPreferences,
            appUtil = fakeAppUtil,
            currentUserFlowOverride = userFlow,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsDevModeStatusAndVersionInfo() = runTest(mainDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2.3.4", state.appVersionInfo)
        assertFalse(state.isDevModeUnlocked)
        assertFalse(state.navigateToDebugMenu)
        assertEquals("Test User", state.user?.displayName)
        assertEquals(com.kotlinfoundation.koko.root.AppConfiguration.PREMIUM_FEATURES_ENABLED, state.canUpgradeToPremium)
    }

    @Test
    fun tapFiveTimesConsecutively_unlocksDevModeAndPersistsState() = runTest(mainDispatcher) {
        advanceUntilIdle()

        // Tap 4 times
        repeat(4) {
            viewModel.onUiEvent(ProfileScreenUiEvent.OnVersionTapped)
        }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDevModeUnlocked)
        assertFalse(viewModel.uiState.value.navigateToDebugMenu)
        assertFalse(userPreferences.getBoolean(UserPreferences.Keys.KEY_IS_DEV_MODE_ENABLED))

        // 5th tap
        viewModel.onUiEvent(ProfileScreenUiEvent.OnVersionTapped)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isDevModeUnlocked)
        assertTrue(state.navigateToDebugMenu)
        assertNotNull(state.feedbackMessage)
        val feedback = state.feedbackMessage
        assertTrue(feedback.contains("Developer mode unlocked"))

        // Verify persistence in preferences
        assertTrue(userPreferences.getBoolean(UserPreferences.Keys.KEY_IS_DEV_MODE_ENABLED))
    }

    @Test
    fun tapWhenAlreadyUnlocked_immediatelyNavigatesToDebugMenu() = runTest(mainDispatcher) {
        advanceUntilIdle()

        // Pre-unlock in preferences
        userPreferences.putBoolean(UserPreferences.Keys.KEY_IS_DEV_MODE_ENABLED, true)

        val unlockedViewModel = ProfileViewModel(
            userPreferences = userPreferences,
            appUtil = fakeAppUtil,
            currentUserFlowOverride = userFlow,
        )
        advanceUntilIdle()
        assertTrue(unlockedViewModel.uiState.value.isDevModeUnlocked)

        // 1 tap is enough when already unlocked
        unlockedViewModel.onUiEvent(ProfileScreenUiEvent.OnVersionTapped)
        advanceUntilIdle()

        assertTrue(unlockedViewModel.uiState.value.navigateToDebugMenu)
    }

    @Test
    fun debugMenuNavigated_resetsNavigationFlag() = runTest(mainDispatcher) {
        advanceUntilIdle()

        repeat(5) {
            viewModel.onUiEvent(ProfileScreenUiEvent.OnVersionTapped)
        }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.navigateToDebugMenu)

        viewModel.onUiEvent(ProfileScreenUiEvent.OnDebugMenuNavigated)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.navigateToDebugMenu)
    }

    @Test
    fun analyticsEvents_areLoggedOnUserActions() = runTest(mainDispatcher) {
        val fakeAnalytics = object : com.kotlinfoundation.koko.util.analytics.Analytics {
            val events = mutableListOf<String>()
            override fun logEvent(event: String, params: Map<String, Any>?) {
                events.add(event)
            }
            override fun setEnabled(enabled: Boolean) {}
        }

        val testVm = ProfileViewModel(
            userPreferences = userPreferences,
            appUtil = fakeAppUtil,
            currentUserFlowOverride = userFlow,
            analytics = fakeAnalytics,
        )

        testVm.onUiEvent(ProfileScreenUiEvent.OnClickDeleteAccount)
        advanceUntilIdle()
        assertTrue(fakeAnalytics.events.contains("delete_account_requested"))

        repeat(5) {
            testVm.onUiEvent(ProfileScreenUiEvent.OnVersionTapped)
        }
        advanceUntilIdle()
        assertTrue(fakeAnalytics.events.contains("dev_mode_unlocked"))
    }
}
