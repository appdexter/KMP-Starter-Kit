@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.kotlinfoundation.koko.presentation.screens.debug

import com.kotlinfoundation.koko.data.source.preferences.FakeUserPreferences
import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.growth.experiment.ExperimentEngine
import com.kotlinfoundation.koko.identity.SessionManager
import kotlinx.coroutines.Dispatchers
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

class DebugMenuViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()
    private lateinit var userPreferences: FakeUserPreferences
    private lateinit var sessionManager: SessionManager
    private lateinit var experimentEngine: ExperimentEngine
    private lateinit var viewModel: DebugMenuViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        userPreferences = FakeUserPreferences()
        sessionManager = SessionManager(initialAnonymousId = "test_anon_id_999")
        experimentEngine = ExperimentEngine(sessionManager, userPreferences)
        viewModel = DebugMenuViewModel(
            sessionManager = sessionManager,
            experimentEngine = experimentEngine,
            userPreferences = userPreferences,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasCorrectDefaults() = runTest(mainDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("test_anon_id_999", state.anonymousId)
        assertEquals("DEV", state.environment)
        assertEquals("1.0.0", state.appVersion)
        assertFalse(state.isForcePremium)
        assertTrue(state.diagnosticsJson.contains("test_anon_id_999"))
        assertTrue(state.diagnosticsJson.contains("Koko"))
    }

    @Test
    fun setForcePremium_updatesStateAndSessionManager() = runTest(mainDispatcher) {
        advanceUntilIdle()

        viewModel.onUiEvent(DebugMenuUiEvent.SetForcePremium(true))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isForcePremium)
        assertTrue(sessionManager.userIdentity.value.isPremium)
        assertTrue(state.diagnosticsJson.contains("\"is_premium\": true"))
        assertNotNull(state.message)

        viewModel.onUiEvent(DebugMenuUiEvent.SetForcePremium(false))
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertFalse(updatedState.isForcePremium)
        assertFalse(sessionManager.userIdentity.value.isPremium)
        assertTrue(updatedState.diagnosticsJson.contains("\"is_premium\": false"))
    }

    @Test
    fun resetOnboarding_updatesPreferencesAndStateMessage() = runTest(mainDispatcher) {
        advanceUntilIdle()

        userPreferences.putBoolean(UserPreferences.Keys.KEY_FIRST_TIME_USER, false)
        userPreferences.putBoolean(UserPreferences.Keys.KEY_IS_ONBOARD_SHOWN, true)

        viewModel.onUiEvent(DebugMenuUiEvent.ResetOnboarding)
        advanceUntilIdle()

        assertTrue(userPreferences.getBoolean(UserPreferences.Keys.KEY_FIRST_TIME_USER))
        assertFalse(userPreferences.getBoolean(UserPreferences.Keys.KEY_IS_ONBOARD_SHOWN))
        assertEquals("Onboarding reset successfully", viewModel.uiState.value.message)
    }

    @Test
    fun overrideExperiment_delegatesToExperimentEngine() = runTest(mainDispatcher) {
        advanceUntilIdle()

        viewModel.onUiEvent(DebugMenuUiEvent.OverrideExperiment("test_exp", "variant_b"))
        advanceUntilIdle()

        assertEquals("variant_b", experimentEngine.getVariant("test_exp"))
        assertTrue(viewModel.uiState.value.message?.contains("variant_b") == true)
    }

    @Test
    fun refreshDiagnostics_updatesDiagnosticsReport() = runTest(mainDispatcher) {
        advanceUntilIdle()

        val initialJson = viewModel.uiState.value.diagnosticsJson
        assertTrue(initialJson.isNotBlank())

        viewModel.onUiEvent(DebugMenuUiEvent.RefreshDiagnostics)
        advanceUntilIdle()

        val refreshedJson = viewModel.uiState.value.diagnosticsJson
        assertTrue(refreshedJson.isNotBlank())
        assertEquals("Diagnostics refreshed", viewModel.uiState.value.message)
    }

    @Test
    fun dismissMessage_clearsMessage() = runTest(mainDispatcher) {
        advanceUntilIdle()

        viewModel.onUiEvent(DebugMenuUiEvent.RefreshDiagnostics)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.message)

        viewModel.onUiEvent(DebugMenuUiEvent.DismissMessage)
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.message)
    }
}
