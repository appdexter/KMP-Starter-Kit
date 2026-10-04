@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.kotlinfoundation.koko.presentation.screens.onboarding

import com.kotlinfoundation.koko.data.source.preferences.FakeUserPreferences
import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.util.analytics.Analytics
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
import kotlin.test.assertTrue

class FakeAnalytics : Analytics {
    val loggedEvents = mutableListOf<Pair<String, Map<String, Any>?>>()
    override fun logEvent(event: String, params: Map<String, Any>?) {
        loggedEvents.add(event to params)
    }
    override fun setEnabled(enabled: Boolean) {}
}

class OnBoardingViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()
    private lateinit var userPreferences: FakeUserPreferences
    private lateinit var analytics: FakeAnalytics

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        userPreferences = FakeUserPreferences()
        analytics = FakeAnalytics()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `first launch shows onboarding and logs step 1`() = runTest(mainDispatcher) {
        val viewModel = OnBoardingViewModel(userPreferences, analytics)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isOnBoardingFinished)
        assertFalse(state.isLoading)
        assertEquals(1, state.currentStep)
        assertTrue(analytics.loggedEvents.any { it.first == Analytics.EVENT_ONBOARDING_STEP })
    }

    @Test
    fun `returning user finishes as an existing user`() = runTest(mainDispatcher) {
        userPreferences.putBoolean(UserPreferences.KEY_IS_ONBOARD_SHOWN, true)

        val viewModel = OnBoardingViewModel(userPreferences, analytics)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isOnBoardingFinished)
        assertFalse(state.isNewUser)
    }

    @Test
    fun `clicking start finishes onboarding as a new user and persists it`() = runTest(mainDispatcher) {
        val viewModel = OnBoardingViewModel(userPreferences, analytics)
        advanceUntilIdle()

        viewModel.onUiEvent(OnBoardingUiEvent.OnClickStart)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isOnBoardingFinished)
        assertTrue(state.isNewUser)
        assertTrue(userPreferences.getBoolean(UserPreferences.KEY_IS_ONBOARD_SHOWN))
        assertTrue(analytics.loggedEvents.any { it.first == Analytics.EVENT_ONBOARDING_COMPLETED })
    }

    @Test
    fun `onFinishHandled resets the finished flag`() = runTest(mainDispatcher) {
        val viewModel = OnBoardingViewModel(userPreferences, analytics)
        advanceUntilIdle()

        viewModel.onUiEvent(OnBoardingUiEvent.OnClickStart)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isOnBoardingFinished)

        viewModel.onFinishHandled()
        assertFalse(viewModel.uiState.value.isOnBoardingFinished)
    }

    @Test
    fun `deep assessment flow selects options, simulates labor illusion, and persists preferences`() = runTest(mainDispatcher) {
        val viewModel = OnBoardingViewModel(userPreferences, analytics)
        advanceUntilIdle()

        // Step 1: Select Goal
        viewModel.onUiEvent(OnBoardingUiEvent.SelectGoal("productivity"))
        assertEquals("productivity", viewModel.uiState.value.selectedGoal)
        viewModel.onUiEvent(OnBoardingUiEvent.NextStep)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentStep)

        // Step 2: Select Barrier
        viewModel.onUiEvent(OnBoardingUiEvent.SelectBarrier("consistency"))
        assertEquals("consistency", viewModel.uiState.value.selectedBarrier)
        viewModel.onUiEvent(OnBoardingUiEvent.NextStep)
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.currentStep)

        // Step 3: Select Commitment
        viewModel.onUiEvent(OnBoardingUiEvent.SelectCommitment(10))
        assertEquals(10, viewModel.uiState.value.dailyMinutes)
        viewModel.onUiEvent(OnBoardingUiEvent.NextStep)
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.currentStep)

        // Step 4: Social Proof -> Next triggers Step 5 (Labor Illusion)
        viewModel.onUiEvent(OnBoardingUiEvent.NextStep)
        // Advance virtual time through the 2500ms labor illusion animation
        advanceUntilIdle()

        // Labor illusion should automatically complete and transition to Step 6
        assertEquals(6, viewModel.uiState.value.currentStep)
        assertFalse(viewModel.uiState.value.isAnalyzing)
        assertEquals(1.0f, viewModel.uiState.value.analysisProgress)

        // Step 6 -> Step 7
        viewModel.onUiEvent(OnBoardingUiEvent.NextStep)
        advanceUntilIdle()
        assertEquals(7, viewModel.uiState.value.currentStep)

        // Step 7: Finish onboarding
        viewModel.onUiEvent(OnBoardingUiEvent.FinishOnBoarding)
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue(finalState.isOnBoardingFinished)
        assertTrue(finalState.isNewUser)

        // Verify preferences were stored
        assertTrue(userPreferences.getBoolean(UserPreferences.KEY_IS_ONBOARD_SHOWN))
        assertEquals("productivity", userPreferences.getString(UserPreferences.KEY_USER_GOAL))
        assertEquals("consistency", userPreferences.getString(UserPreferences.KEY_USER_BARRIER))
        assertEquals(10, userPreferences.getInt(UserPreferences.KEY_DAILY_COMMITMENT))

        // Verify analytics
        assertTrue(analytics.loggedEvents.any { it.first == Analytics.EVENT_ONBOARDING_COMPLETED })
    }

    @Test
    fun `navigating backward skips labor illusion`() = runTest(mainDispatcher) {
        val viewModel = OnBoardingViewModel(userPreferences, analytics)
        advanceUntilIdle()

        // Go forward to step 4
        viewModel.onUiEvent(OnBoardingUiEvent.NextStep) // 2
        viewModel.onUiEvent(OnBoardingUiEvent.NextStep) // 3
        viewModel.onUiEvent(OnBoardingUiEvent.NextStep) // 4
        viewModel.onUiEvent(OnBoardingUiEvent.NextStep) // 5 -> auto to 6
        advanceUntilIdle()

        assertEquals(6, viewModel.uiState.value.currentStep)

        // Previous step from 6 should jump back to 4 (skipping automated step 5)
        viewModel.onUiEvent(OnBoardingUiEvent.PreviousStep)
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.currentStep)
    }
}
