package com.kotlinfoundation.koko.presentation.screens.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinfoundation.koko.designsystem.generated.resources.UiRes
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_back
import com.kotlinfoundation.koko.designsystem.theme.AppTheme
import com.kotlinfoundation.koko.presentation.screens.onboarding.components.BarrierSelectionStep
import com.kotlinfoundation.koko.presentation.screens.onboarding.components.CommitmentStep
import com.kotlinfoundation.koko.presentation.screens.onboarding.components.GoalSelectionStep
import com.kotlinfoundation.koko.presentation.screens.onboarding.components.LaborIllusionStep
import com.kotlinfoundation.koko.presentation.screens.onboarding.components.LossAversionStep
import com.kotlinfoundation.koko.presentation.screens.onboarding.components.PaywallHandoffStep
import com.kotlinfoundation.koko.presentation.screens.onboarding.components.SocialProofStep
import org.jetbrains.compose.resources.painterResource

/**
 * Entry point with ViewModel.
 */
@Composable
fun DeepAssessmentOnBoardingScreen(
    viewModel: OnBoardingViewModel,
    modifier: Modifier = Modifier,
    onNavigateToPaywall: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isOnBoardingFinished) {
        if (uiState.isOnBoardingFinished) {
            onNavigateToPaywall()
            viewModel.onFinishHandled()
        }
    }

    DeepAssessmentOnBoardingScreen(uiState = uiState, onUiEvent = viewModel::onUiEvent, modifier = modifier)
}

/**
 * Pure composable for testing and previews.
 */
@Composable
fun DeepAssessmentOnBoardingScreen(
    uiState: OnBoardingUiState,
    onUiEvent: (OnBoardingUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToPaywall: () -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxSize().background(AppTheme.colors.background).safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            TopProgressBar(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
                showBackButton = uiState.currentStep > 1 && uiState.currentStep != AssessmentStep.LABOR_ILLUSION.index,
                onBack = { onUiEvent(OnBoardingUiEvent.PreviousStep) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (uiState.currentStep) {
                    AssessmentStep.HOOK_GOAL.index -> GoalSelectionStep(
                        goalOptions = uiState.goalOptions,
                        selectedGoal = uiState.selectedGoal,
                        onSelect = { onUiEvent(OnBoardingUiEvent.SelectGoal(it)) },
                        onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                    )

                    AssessmentStep.PAIN_POINT.index -> BarrierSelectionStep(
                        barrierOptions = uiState.barrierOptions,
                        selectedBarrier = uiState.selectedBarrier,
                        onSelect = { onUiEvent(OnBoardingUiEvent.SelectBarrier(it)) },
                        onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                    )

                    AssessmentStep.MICRO_COMMITMENT.index -> CommitmentStep(
                        commitmentOptions = uiState.commitmentOptions,
                        selectedMinutes = uiState.dailyMinutes,
                        onSelect = { onUiEvent(OnBoardingUiEvent.SelectCommitment(it)) },
                        onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                    )

                    AssessmentStep.SOCIAL_PROOF.index -> SocialProofStep(
                        testimonials = uiState.testimonials,
                        onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                    )

                    AssessmentStep.LABOR_ILLUSION.index -> LaborIllusionStep(
                        progress = uiState.analysisProgress,
                        statusText = uiState.analysisStatusText,
                        dailyMinutes = uiState.dailyMinutes ?: 10,
                    )

                    AssessmentStep.LOSS_AVERSION.index -> LossAversionStep(
                        dailyMinutes = uiState.dailyMinutes ?: 10,
                        onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                    )

                    AssessmentStep.PAYWALL_HANDOFF.index -> PaywallHandoffStep(
                        uiState = uiState,
                        onUnlock = { onUiEvent(OnBoardingUiEvent.FinishOnBoarding) },
                    )

                    else -> GoalSelectionStep(
                        goalOptions = uiState.goalOptions,
                        selectedGoal = uiState.selectedGoal,
                        onSelect = { onUiEvent(OnBoardingUiEvent.SelectGoal(it)) },
                        onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopProgressBar(
    currentStep: Int,
    totalSteps: Int,
    showBackButton: Boolean,
    onBack: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = currentStep.toFloat() / totalSteps.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (showBackButton) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(painter = painterResource(UiRes.drawable.ic_back), contentDescription = "Back", tint = AppTheme.colors.text.primary, modifier = Modifier.size(20.dp))
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }

            Text(
                text = "Step $currentStep of $totalSteps",
                style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = AppTheme.colors.text.secondary,
            )

            Spacer(modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(AppTheme.colors.outline.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(fraction = progress.coerceIn(0.05f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(AppTheme.colors.primary, AppTheme.colors.primary.copy(alpha = 0.85f)))),
            )
        }
    }
}

@Preview
@Composable
private fun DeepAssessmentStep1Preview() {
    AppTheme { DeepAssessmentOnBoardingScreen(uiState = OnBoardingUiState(currentStep = 1, selectedGoal = "productivity", isLoading = false), onUiEvent = {}) }
}

@Preview
@Composable
private fun DeepAssessmentStep3Preview() {
    AppTheme { DeepAssessmentOnBoardingScreen(uiState = OnBoardingUiState(currentStep = 3, dailyMinutes = 10, isLoading = false), onUiEvent = {}) }
}

@Preview
@Composable
private fun DeepAssessmentStep6Preview() {
    AppTheme { DeepAssessmentOnBoardingScreen(uiState = OnBoardingUiState(currentStep = 6, dailyMinutes = 10, isLoading = false), onUiEvent = {}) }
}

@Preview
@Composable
private fun DeepAssessmentStep7Preview() {
    AppTheme { DeepAssessmentOnBoardingScreen(uiState = OnBoardingUiState(currentStep = 7, selectedGoal = "productivity", selectedBarrier = "consistency", dailyMinutes = 10, isLoading = false), onUiEvent = {}) }
}
