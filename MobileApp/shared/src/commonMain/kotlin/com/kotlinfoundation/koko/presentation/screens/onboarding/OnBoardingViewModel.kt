package com.kotlinfoundation.koko.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.util.analytics.Analytics
import com.kotlinfoundation.koko.util.analytics.NoImplAnalytics
import com.kotlinfoundation.koko.util.analytics.logOnboardingCompleted
import com.kotlinfoundation.koko.util.analytics.logOnboardingStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnBoardingViewModel(
    private val userPreferences: UserPreferences,
    private val analytics: Analytics = NoImplAnalytics,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnBoardingUiState(isLoading = true))
    val uiState: StateFlow<OnBoardingUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null

    init {
        checkIfOnBoardIsShown()
    }

    fun onUiEvent(event: OnBoardingUiEvent) {
        when (event) {
            is OnBoardingUiEvent.SelectGoal -> {
                _uiState.update { it.copy(selectedGoal = event.goal) }
            }

            is OnBoardingUiEvent.SelectBarrier -> {
                _uiState.update { it.copy(selectedBarrier = event.barrier) }
            }

            is OnBoardingUiEvent.SelectCommitment -> {
                _uiState.update { it.copy(dailyMinutes = event.minutes) }
            }

            OnBoardingUiEvent.NextStep -> {
                handleNextStep()
            }

            OnBoardingUiEvent.PreviousStep -> {
                handlePreviousStep()
            }

            OnBoardingUiEvent.FinishOnBoarding -> {
                finishOnboarding()
            }

            OnBoardingUiEvent.OnClickStart -> {
                finishOnboarding()
            }
        }
    }

    private fun handleNextStep() {
        val current = _uiState.value.currentStep
        if (current < _uiState.value.totalSteps) {
            val nextStep = current + 1
            _uiState.update { it.copy(currentStep = nextStep) }
            analytics.logOnboardingStep(
                step = nextStep,
                stepName = AssessmentStep.fromIndex(nextStep).title,
            )

            if (nextStep == AssessmentStep.LABOR_ILLUSION.index) {
                startLaborIllusion()
            }
        } else {
            finishOnboarding()
        }
    }

    private fun handlePreviousStep() {
        analysisJob?.cancel()
        val current = _uiState.value.currentStep
        if (current > 1) {
            val prevStep = if (current == AssessmentStep.LOSS_AVERSION.index) {
                AssessmentStep.SOCIAL_PROOF.index
            } else {
                current - 1
            }
            _uiState.update { it.copy(currentStep = prevStep, isAnalyzing = false) }
            analytics.logOnboardingStep(
                step = prevStep,
                stepName = AssessmentStep.fromIndex(prevStep).title,
            )
        }
    }

    private fun startLaborIllusion() {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    analysisProgress = 0f,
                    analysisStatusText = "Analyzing your primary goal & obstacles...",
                )
            }

            val totalDurationMs = 2500L
            val intervals = 25
            val stepDelay = totalDurationMs / intervals

            for (i in 1..intervals) {
                delay(stepDelay)
                val progress = i.toFloat() / intervals
                val statusText = when {
                    progress < 0.35f -> "Analyzing your primary goal & obstacles..."
                    progress < 0.70f -> "Synthesizing behavioral patterns & habits..."
                    else -> "Calibrating 30-day success roadmap..."
                }
                _uiState.update {
                    it.copy(
                        analysisProgress = progress,
                        analysisStatusText = statusText,
                    )
                }
            }

            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    currentStep = AssessmentStep.LOSS_AVERSION.index,
                )
            }
            analytics.logOnboardingStep(
                step = AssessmentStep.LOSS_AVERSION.index,
                stepName = AssessmentStep.LOSS_AVERSION.title,
            )
        }
    }

    private fun finishOnboarding() = viewModelScope.launch {
        userPreferences.putBoolean(UserPreferences.KEY_IS_ONBOARD_SHOWN, true)
        _uiState.value.selectedGoal?.let {
            userPreferences.putString(UserPreferences.KEY_USER_GOAL, it)
        }
        _uiState.value.selectedBarrier?.let {
            userPreferences.putString(UserPreferences.KEY_USER_BARRIER, it)
        }
        _uiState.value.dailyMinutes?.let {
            userPreferences.putInt(UserPreferences.KEY_DAILY_COMMITMENT, it)
        }

        analytics.logOnboardingCompleted(
            goal = _uiState.value.selectedGoal,
            barrier = _uiState.value.selectedBarrier,
            dailyCommitment = _uiState.value.dailyMinutes,
        )

        _uiState.update { it.copy(isOnBoardingFinished = true, isNewUser = true) }
    }

    fun onFinishHandled() = _uiState.update { it.copy(isOnBoardingFinished = false) }

    private fun checkIfOnBoardIsShown() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        if (userPreferences.getBoolean(UserPreferences.KEY_IS_ONBOARD_SHOWN)) {
            _uiState.update { it.copy(isOnBoardingFinished = true, isNewUser = false) }
        } else {
            _uiState.update { it.copy(isLoading = false) }
            analytics.logOnboardingStep(
                step = 1,
                stepName = AssessmentStep.HOOK_GOAL.title,
            )
        }
    }
}
