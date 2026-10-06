package com.kotlinfoundation.koko.presentation.screens.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinfoundation.koko.designsystem.components.AppButton
import com.kotlinfoundation.koko.designsystem.components.ButtonSize
import com.kotlinfoundation.koko.designsystem.components.ButtonStyle
import com.kotlinfoundation.koko.designsystem.theme.AppTheme
import com.kotlinfoundation.koko.presentation.screens.onboarding.OnBoardingUiState

@Composable
fun PaywallHandoffStep(
    uiState: OnBoardingUiState,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedGoalTitle = uiState.goalOptions.firstOrNull { it.id == uiState.selectedGoal }?.title
        ?: "Maximize Daily Productivity"
    val selectedBarrierTitle = uiState.barrierOptions.firstOrNull { it.id == uiState.selectedBarrier }?.title
        ?: "Lack of Consistency"
    val dailyCommitmentMinutes = uiState.dailyMinutes ?: 10

    Column(
        modifier = modifier
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
fun PlanSummaryItem(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
