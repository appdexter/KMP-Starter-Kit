package com.kotlinfoundation.koko.presentation.screens.paywall.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinfoundation.koko.designsystem.generated.resources.UiRes
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_check
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_sparkles
import com.kotlinfoundation.koko.designsystem.theme.AppTheme
import com.kotlinfoundation.koko.generated.resources.Res
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_badge
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_commitment
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_headline_barrier
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_headline_both
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_headline_default
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_headline_goal
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_phase_1
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_phase_1_label
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_phase_2
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_phase_2_label
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_phase_3
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_phase_3_label
import com.kotlinfoundation.koko.generated.resources.paywall_personalized_subtitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun formatGoalText(goal: String?): String = when (goal?.lowercase()?.trim()) {
    "productivity" -> "Productivity"

    "creativity" -> "Creative Habits"

    "growth" -> "Skill Mastery"

    "clarity" -> "Mental Clarity"

    null, "" -> "Your Goal"

    else -> goal.replace('_', ' ').split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

fun formatBarrierText(barrier: String?): String = when (barrier?.lowercase()?.trim()) {
    "consistency" -> "Inconsistency"

    "distractions" -> "Distractions"

    "no_structure" -> "Lack of Structure"

    "limited_time" -> "Time Constraints"

    null, "" -> "Obstacles"

    else -> barrier.replace('_', ' ').split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

/**
 * Personalized header displayed on the Paywall based on the user's Onboarding Quiz answers.
 * Shows tailored messaging addressing their specific goal, primary barrier, and daily commitment.
 */
@Composable
fun PersonalizedPlanHeader(
    goal: String?,
    barrier: String?,
    dailyMinutes: Int?,
    modifier: Modifier = Modifier,
) {
    val formattedGoal = formatGoalText(goal)
    val formattedBarrier = formatBarrierText(barrier)

    val headline = when {
        !barrier.isNullOrBlank() && !goal.isNullOrBlank() ->
            stringResource(Res.string.paywall_personalized_headline_both, formattedBarrier, formattedGoal)

        !goal.isNullOrBlank() ->
            stringResource(Res.string.paywall_personalized_headline_goal, formattedGoal)

        !barrier.isNullOrBlank() ->
            stringResource(Res.string.paywall_personalized_headline_barrier, formattedBarrier)

        else ->
            stringResource(Res.string.paywall_personalized_headline_default)
    }

    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colors.primary.copy(alpha = 0.09f),
                        AppTheme.colors.surfaceContainer,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = AppTheme.colors.primary.copy(alpha = 0.22f),
                shape = cardShape,
            )
            .padding(AppTheme.spacing.cardContentSpacing),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.groupedVerticalElementSpacing),
        ) {
            // Personalized Plan Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AppTheme.colors.primary.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = AppTheme.colors.primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(UiRes.drawable.ic_sparkles),
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = stringResource(Res.string.paywall_personalized_badge),
                    style = AppTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = AppTheme.colors.primary,
                )
            }

            // Headline
            Text(
                text = headline,
                style = AppTheme.typography.h4,
                color = AppTheme.colors.text.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            // Subtitle description
            Text(
                text = stringResource(Res.string.paywall_personalized_subtitle),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // Daily commitment badge if available
            if (dailyMinutes != null && dailyMinutes > 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.colors.surfaceContainer)
                        .border(
                            width = 1.dp,
                            color = AppTheme.colors.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "⏱️",
                        fontSize = 14.sp,
                    )
                    Text(
                        text = stringResource(Res.string.paywall_personalized_commitment, dailyMinutes),
                        style = AppTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = AppTheme.colors.text.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 3-Phase Milestone Roadmap Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppTheme.colors.primary.copy(alpha = 0.04f))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MilestoneStep(
                    phase = stringResource(Res.string.paywall_personalized_phase_1),
                    label = stringResource(Res.string.paywall_personalized_phase_1_label),
                    isCurrent = true,
                )
                Text(
                    text = "→",
                    color = AppTheme.colors.text.secondary.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                MilestoneStep(
                    phase = stringResource(Res.string.paywall_personalized_phase_2),
                    label = stringResource(Res.string.paywall_personalized_phase_2_label),
                    isCurrent = false,
                )
                Text(
                    text = "→",
                    color = AppTheme.colors.text.secondary.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                MilestoneStep(
                    phase = stringResource(Res.string.paywall_personalized_phase_3),
                    label = stringResource(Res.string.paywall_personalized_phase_3_label),
                    isCurrent = false,
                )
            }
        }
    }
}

@Composable
private fun MilestoneStep(
    phase: String,
    label: String,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isCurrent) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.2f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isCurrent) {
                Icon(
                    painter = painterResource(UiRes.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.text.secondary),
                )
            }
        }
        Text(
            text = phase,
            style = AppTheme.typography.bodyExtraSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            ),
            color = if (isCurrent) AppTheme.colors.primary else AppTheme.colors.text.secondary,
        )
        Text(
            text = label,
            style = AppTheme.typography.bodyExtraSmall.copy(
                fontSize = 10.sp,
            ),
            color = AppTheme.colors.text.secondary,
        )
    }
}

@Preview
@Composable
private fun PersonalizedPlanHeaderPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PersonalizedPlanHeader(
                goal = "creativity",
                barrier = "limited_time",
                dailyMinutes = 15,
            )
        }
    }
}

@Preview
@Composable
private fun PersonalizedPlanHeaderMinimalPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PersonalizedPlanHeader(
                goal = null,
                barrier = null,
                dailyMinutes = null,
            )
        }
    }
}
