package com.kotlinfoundation.koko.presentation.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinfoundation.koko.designsystem.components.AppButton
import com.kotlinfoundation.koko.designsystem.components.ButtonSize
import com.kotlinfoundation.koko.designsystem.components.ButtonStyle
import com.kotlinfoundation.koko.designsystem.generated.resources.UiRes
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_arrow_right
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_back
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_check
import com.kotlinfoundation.koko.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.painterResource

/**
 * Entry point with ViewModel.
 */
@Composable
fun DeepAssessmentOnBoardingScreen(
    viewModel: OnBoardingViewModel,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isOnBoardingFinished) {
        if (uiState.isOnBoardingFinished) {
            onNavigateToPaywall()
            viewModel.onFinishHandled()
        }
    }

    DeepAssessmentOnBoardingScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        onNavigateToPaywall = onNavigateToPaywall,
        modifier = modifier,
    )
}

/**
 * Pure composable for testing and previews.
 */
@Composable
fun DeepAssessmentOnBoardingScreen(
    uiState: OnBoardingUiState,
    onUiEvent: (OnBoardingUiEvent) -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Top Navigation & Progress Header
            TopProgressBar(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
                showBackButton = uiState.currentStep > 1 && uiState.currentStep != AssessmentStep.LABOR_ILLUSION.index,
                onBack = { onUiEvent(OnBoardingUiEvent.PreviousStep) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (uiState.currentStep) {
                    AssessmentStep.HOOK_GOAL.index -> {
                        GoalSelectionStep(
                            goalOptions = uiState.goalOptions,
                            selectedGoal = uiState.selectedGoal,
                            onSelect = { onUiEvent(OnBoardingUiEvent.SelectGoal(it)) },
                            onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                        )
                    }

                    AssessmentStep.PAIN_POINT.index -> {
                        BarrierSelectionStep(
                            barrierOptions = uiState.barrierOptions,
                            selectedBarrier = uiState.selectedBarrier,
                            onSelect = { onUiEvent(OnBoardingUiEvent.SelectBarrier(it)) },
                            onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                        )
                    }

                    AssessmentStep.MICRO_COMMITMENT.index -> {
                        CommitmentStep(
                            commitmentOptions = uiState.commitmentOptions,
                            selectedMinutes = uiState.dailyMinutes,
                            onSelect = { onUiEvent(OnBoardingUiEvent.SelectCommitment(it)) },
                            onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                        )
                    }

                    AssessmentStep.SOCIAL_PROOF.index -> {
                        SocialProofStep(
                            testimonials = uiState.testimonials,
                            onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                        )
                    }

                    AssessmentStep.LABOR_ILLUSION.index -> {
                        LaborIllusionStep(
                            progress = uiState.analysisProgress,
                            statusText = uiState.analysisStatusText,
                            dailyMinutes = uiState.dailyMinutes ?: 10,
                        )
                    }

                    AssessmentStep.LOSS_AVERSION.index -> {
                        LossAversionStep(
                            dailyMinutes = uiState.dailyMinutes ?: 10,
                            onNext = { onUiEvent(OnBoardingUiEvent.NextStep) },
                        )
                    }

                    AssessmentStep.PAYWALL_HANDOFF.index -> {
                        PaywallHandoffStep(
                            uiState = uiState,
                            onUnlock = {
                                onUiEvent(OnBoardingUiEvent.FinishOnBoarding)
                                onNavigateToPaywall()
                            },
                        )
                    }

                    else -> {
                        GoalSelectionStep(
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
}

// ─────────────────────────────────────────────────────────────────────────────
// Top Progress Bar
// ─────────────────────────────────────────────────────────────────────────────

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
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (showBackButton) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        painter = painterResource(UiRes.drawable.ic_back),
                        contentDescription = "Back",
                        tint = AppTheme.colors.text.primary,
                        modifier = Modifier.size(20.dp),
                    )
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
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppTheme.colors.outline.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0.05f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppTheme.colors.primary,
                                AppTheme.colors.primary.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1: Hook & Primary Goal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GoalSelectionStep(
    goalOptions: List<GoalOption>,
    selectedGoal: String?,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "What is your primary goal?",
            style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = AppTheme.colors.text.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose what you want to achieve. We'll tailor your personalized progression path.",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.text.secondary,
        )
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            goalOptions.forEach { goal ->
                val isSelected = selectedGoal == goal.id
                SelectableOptionCard(
                    title = goal.title,
                    subtitle = goal.subtitle,
                    iconEmoji = goal.iconEmoji,
                    isSelected = isSelected,
                    onClick = { onSelect(goal.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AppButton(
            text = "Continue",
            style = ButtonStyle.PRIMARY,
            size = ButtonSize.MEDIUM,
            enabled = selectedGoal != null,
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2: Pain Point Identification
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BarrierSelectionStep(
    barrierOptions: List<BarrierOption>,
    selectedBarrier: String?,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "What holds you back the most?",
            style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = AppTheme.colors.text.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Identifying friction points is the key to creating sustainable daily habits.",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.text.secondary,
        )
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            barrierOptions.forEach { barrier ->
                val isSelected = selectedBarrier == barrier.id
                SelectableOptionCard(
                    title = barrier.title,
                    subtitle = barrier.subtitle,
                    iconEmoji = barrier.iconEmoji,
                    isSelected = isSelected,
                    onClick = { onSelect(barrier.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AppButton(
            text = "Continue",
            style = ButtonStyle.PRIMARY,
            size = ButtonSize.MEDIUM,
            enabled = selectedBarrier != null,
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3: Micro-Commitment
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CommitmentStep(
    commitmentOptions: List<CommitmentOption>,
    selectedMinutes: Int?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "How much time can you commit daily?",
            style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = AppTheme.colors.text.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Small, consistent micro-commitments yield 10x higher long-term consistency.",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.text.secondary,
        )
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            commitmentOptions.forEach { option ->
                val isSelected = selectedMinutes == option.minutes
                val borderColor by animateColorAsState(
                    if (isSelected) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.3f),
                )
                val bgColor by animateColorAsState(
                    if (isSelected) AppTheme.colors.primary.copy(alpha = 0.08f) else AppTheme.colors.surfaceContainer,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable { onSelect(option.minutes) }
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = option.title,
                                    style = AppTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.text.primary,
                                )
                                if (option.tag != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (option.tag == "Recommended") AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.2f),
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = option.tag,
                                            style = AppTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = if (option.tag == "Recommended") AppTheme.colors.onPrimary else AppTheme.colors.text.secondary,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = option.subtitle,
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.text.secondary,
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                )
                                .background(if (isSelected) AppTheme.colors.primary else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(UiRes.drawable.ic_check),
                                    contentDescription = null,
                                    tint = AppTheme.colors.onPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AppButton(
            text = "Continue",
            style = ButtonStyle.PRIMARY,
            size = ButtonSize.MEDIUM,
            enabled = selectedMinutes != null,
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4: Social Proof & Testimonials
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SocialProofStep(
    testimonials: List<TestimonialItem>,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Backed by Behavioral Science",
            style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = AppTheme.colors.text.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Join thousands of members who unlocked peak focus and daily momentum.",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.text.secondary,
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Big Stat Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppTheme.colors.primary.copy(alpha = 0.15f),
                            AppTheme.colors.primary.copy(alpha = 0.05f),
                        ),
                    ),
                )
                .border(1.dp, AppTheme.colors.primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "87%",
                    style = AppTheme.typography.h1.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = AppTheme.colors.primary,
                )
                Text(
                    text = "of members achieve their target habit in under 30 days",
                    style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.text.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★★★★★",
                        style = AppTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = Color(0xFFFFB800),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "4.9 / 5.0 (12,400+ reviews)",
                        style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = AppTheme.colors.text.secondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Testimonial Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            testimonials.forEach { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppTheme.colors.surfaceContainer)
                        .border(1.dp, AppTheme.colors.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = item.name,
                                    style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.text.primary,
                                )
                                Text(
                                    text = item.role,
                                    style = AppTheme.typography.bodySmall,
                                    color = AppTheme.colors.text.secondary,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppTheme.colors.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = item.highlight,
                                    style = AppTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.primary,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${item.quote}\"",
                            style = AppTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = AppTheme.colors.text.secondary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AppButton(
            text = "Build My Custom Plan",
            style = ButtonStyle.PRIMARY,
            size = ButtonSize.MEDIUM,
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 5: Labor Illusion (Simulated AI Analysis)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LaborIllusionStep(
    progress: Float,
    statusText: String,
    dailyMinutes: Int,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Personalizing Your Plan...",
            style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = AppTheme.colors.text.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Analyzing your profile inputs with our behavioral engine",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Center Animated Progress Circle
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pulseScale),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(160.dp),
                color = AppTheme.colors.outline.copy(alpha = 0.2f),
                strokeWidth = 10.dp,
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(160.dp),
                color = AppTheme.colors.primary,
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = AppTheme.typography.h2.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = AppTheme.colors.text.primary,
                )
                Text(
                    text = "Optimizing",
                    style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = AppTheme.colors.text.secondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Dynamic Status Text
        Text(
            text = statusText,
            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Assessment checklist items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ChecklistRow(
                label = "Analyzing friction points & primary goal",
                isCompleted = progress > 0.30f,
            )
            ChecklistRow(
                label = "Calibrating $dailyMinutes min/day micro-schedule",
                isCompleted = progress > 0.65f,
            )
            ChecklistRow(
                label = "Synthesizing 90-day compounding roadmap",
                isCompleted = progress > 0.90f,
            )
        }
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    isCompleted: Boolean,
) {
    val checkColor by animateColorAsState(
        if (isCompleted) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.4f),
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isCompleted) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted) {
                Icon(
                    painter = painterResource(UiRes.drawable.ic_check),
                    contentDescription = null,
                    tint = AppTheme.colors.onPrimary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = AppTheme.typography.bodySmall.copy(fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal),
            color = if (isCompleted) AppTheme.colors.text.primary else AppTheme.colors.text.secondary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 6: Loss Aversion Comparison
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LossAversionStep(
    dailyMinutes: Int,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "The Cost of Inconsistency",
            style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = AppTheme.colors.text.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Small daily habits compound over 90 days. Here is what your trajectory looks like.",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.text.secondary,
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Visual Canvas Trajectory Chart
        TrajectoryComparisonChart(modifier = Modifier.fillMaxWidth().height(180.dp))

        Spacer(modifier = Modifier.height(20.dp))

        // Contrast Comparison Cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Negative Trajectory (Status Quo)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFE5E5).copy(alpha = 0.35f))
                    .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "❌", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Status Quo (Without Structured Plan)",
                            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFD32F2F),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Erratic starts followed by motivation dip after 4 days\n• Fragmented focus and high mental friction\n• 80% risk of abandonment within the month",
                        style = AppTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = AppTheme.colors.text.secondary,
                    )
                }
            }

            // Positive Trajectory (With Custom Plan)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.primary.copy(alpha = 0.08f))
                    .border(1.5.dp, AppTheme.colors.primary, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🚀", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "With Your $dailyMinutes-Min Daily Routine",
                            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Automatic daily triggers eliminate decision fatigue\n• Measurable milestone progress every 7 days\n• 87% milestone attainment backed by habit science",
                        style = AppTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = AppTheme.colors.text.primary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AppButton(
            text = "Review My Custom Plan",
            style = ButtonStyle.PRIMARY,
            size = ButtonSize.MEDIUM,
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TrajectoryComparisonChart(modifier: Modifier = Modifier) {
    val primaryColor = AppTheme.colors.primary
    val negativeColor = Color(0xFFFF5252)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surfaceContainer)
            .border(1.dp, AppTheme.colors.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Background Grid Lines
            val gridColor = Color.Gray.copy(alpha = 0.15f)
            drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(0f, height * 0.50f), Offset(width, height * 0.50f), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = 1.dp.toPx())

            // Path 1: Negative/Flat Status Quo Trajectory (Dashed)
            val pathStatusQuo = Path().apply {
                moveTo(0f, height * 0.70f)
                cubicTo(
                    width * 0.25f,
                    height * 0.65f,
                    width * 0.50f,
                    height * 0.85f,
                    width,
                    height * 0.88f,
                )
            }
            drawPath(
                path = pathStatusQuo,
                color = negativeColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
                ),
            )

            // Path 2: Positive Compounding Curve (Solid Vibrant)
            val pathSuccess = Path().apply {
                moveTo(0f, height * 0.70f)
                cubicTo(
                    width * 0.35f,
                    height * 0.68f,
                    width * 0.65f,
                    height * 0.35f,
                    width,
                    height * 0.12f,
                )
            }
            drawPath(
                path = pathSuccess,
                color = primaryColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
            )

            // Draw glowing end dot on Success Curve
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx(),
                center = Offset(width, height * 0.12f),
            )
        }

        // Floating Legend Labels
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Day 1",
                style = AppTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = AppTheme.colors.text.secondary,
            )
            Text(
                text = "Day 30",
                style = AppTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = AppTheme.colors.text.secondary,
            )
            Text(
                text = "Day 90 (10x Output)",
                style = AppTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = primaryColor,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 7: Ready for Paywall Handoff
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaywallHandoffStep(
    uiState: OnBoardingUiState,
    onUnlock: () -> Unit,
) {
    val selectedGoalTitle = uiState.goalOptions.firstOrNull { it.id == uiState.selectedGoal }?.title
        ?: "Maximize Daily Productivity"
    val selectedBarrierTitle = uiState.barrierOptions.firstOrNull { it.id == uiState.selectedBarrier }?.title
        ?: "Lack of Consistency"
    val dailyCommitmentMinutes = uiState.dailyMinutes ?: 10

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Your Plan is Ready!",
            style = AppTheme.typography.h3.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold),
            color = AppTheme.colors.text.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Engineered specifically around your goals and available time.",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.text.secondary,
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Plan Summary Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.colors.surfaceContainer)
                .border(1.5.dp, AppTheme.colors.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "CUSTOM ASSESSMENT",
                        style = AppTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = AppTheme.colors.primary,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "87% Match",
                            style = AppTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32),
                        )
                    }
                }

                PlanSummaryItem(label = "Primary Focus", value = selectedGoalTitle, icon = "🎯")
                PlanSummaryItem(label = "Obstacle Addressed", value = selectedBarrierTitle, icon = "🛡️")
                PlanSummaryItem(
                    label = "Target Daily Routine",
                    value = "$dailyCommitmentMinutes Minutes / Day",
                    icon = "⏱️",
                )
                PlanSummaryItem(
                    label = "First Target Milestone",
                    value = "Day 7 Streak — Momentum Lock",
                    icon = "🏆",
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Value Proposition Callout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppTheme.colors.primary.copy(alpha = 0.08f))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "✨", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Activate full access to unlock your personalized milestones, trackers, and smart streak reminders.",
                    style = AppTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = AppTheme.colors.text.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AppButton(
            text = "Unlock My Custom Plan",
            style = ButtonStyle.PRIMARY,
            size = ButtonSize.LARGE,
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlanSummaryItem(
    label: String,
    value: String,
    icon: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.text.secondary,
            )
            Text(
                text = value,
                style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppTheme.colors.text.primary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Component: SelectableOptionCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SelectableOptionCard(
    title: String,
    subtitle: String,
    iconEmoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        if (isSelected) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.3f),
    )
    val bgColor by animateColorAsState(
        if (isSelected) AppTheme.colors.primary.copy(alpha = 0.08f) else AppTheme.colors.surfaceContainer,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.outline.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = iconEmoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.text.primary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.text.secondary,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.5f),
                        shape = CircleShape,
                    )
                    .background(if (isSelected) AppTheme.colors.primary else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(UiRes.drawable.ic_check),
                        contentDescription = null,
                        tint = AppTheme.colors.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews for Roborazzi & IDE
// ─────────────────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun DeepAssessmentStep1Preview() {
    AppTheme {
        DeepAssessmentOnBoardingScreen(
            uiState = OnBoardingUiState(
                currentStep = 1,
                selectedGoal = "productivity",
                isLoading = false,
            ),
            onUiEvent = {},
            onNavigateToPaywall = {},
        )
    }
}

@Preview
@Composable
private fun DeepAssessmentStep3Preview() {
    AppTheme {
        DeepAssessmentOnBoardingScreen(
            uiState = OnBoardingUiState(
                currentStep = 3,
                dailyMinutes = 10,
                isLoading = false,
            ),
            onUiEvent = {},
            onNavigateToPaywall = {},
        )
    }
}

@Preview
@Composable
private fun DeepAssessmentStep6Preview() {
    AppTheme {
        DeepAssessmentOnBoardingScreen(
            uiState = OnBoardingUiState(
                currentStep = 6,
                dailyMinutes = 10,
                isLoading = false,
            ),
            onUiEvent = {},
            onNavigateToPaywall = {},
        )
    }
}

@Preview
@Composable
private fun DeepAssessmentStep7Preview() {
    AppTheme {
        DeepAssessmentOnBoardingScreen(
            uiState = OnBoardingUiState(
                currentStep = 7,
                selectedGoal = "productivity",
                selectedBarrier = "consistency",
                dailyMinutes = 10,
                isLoading = false,
            ),
            onUiEvent = {},
            onNavigateToPaywall = {},
        )
    }
}
