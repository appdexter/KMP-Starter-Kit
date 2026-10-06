package com.kotlinfoundation.koko.presentation.screens.onboarding.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.kotlinfoundation.koko.designsystem.generated.resources.UiRes
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_check
import com.kotlinfoundation.koko.designsystem.theme.AppTheme
import com.kotlinfoundation.koko.presentation.screens.onboarding.BarrierOption
import com.kotlinfoundation.koko.presentation.screens.onboarding.CommitmentOption
import com.kotlinfoundation.koko.presentation.screens.onboarding.GoalOption
import org.jetbrains.compose.resources.painterResource

@Composable
fun GoalSelectionStep(
    goalOptions: List<GoalOption>,
    selectedGoal: String?,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("What is your primary goal?", style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = AppTheme.colors.text.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Choose what you want to achieve. We'll tailor your personalized progression path.", style = AppTheme.typography.bodyMedium, color = AppTheme.colors.text.secondary)
        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f, fill = false)) {
            goalOptions.forEach { goal ->
                GoalOptionCard(
                    goal = goal,
                    isSelected = selectedGoal == goal.id,
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

@Composable
fun BarrierSelectionStep(
    barrierOptions: List<BarrierOption>,
    selectedBarrier: String?,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("What holds you back the most?", style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = AppTheme.colors.text.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Identifying friction points is the key to creating sustainable daily habits.", style = AppTheme.typography.bodyMedium, color = AppTheme.colors.text.secondary)
        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f, fill = false)) {
            barrierOptions.forEach { barrier ->
                BarrierOptionCard(
                    barrier = barrier,
                    isSelected = selectedBarrier == barrier.id,
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

@Composable
fun CommitmentStep(
    commitmentOptions: List<CommitmentOption>,
    selectedMinutes: Int?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("How much time can you commit daily?", style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = AppTheme.colors.text.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Small, consistent micro-commitments yield 10x higher long-term consistency.", style = AppTheme.typography.bodyMedium, color = AppTheme.colors.text.secondary)
        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f, fill = false)) {
            commitmentOptions.forEach { option ->
                CommitmentOptionCard(
                    option = option,
                    isSelected = selectedMinutes == option.minutes,
                    onClick = { onSelect(option.minutes) },
                )
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

@Composable
fun GoalOptionCard(
    goal: GoalOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableOptionCard(
        title = goal.title,
        subtitle = goal.subtitle,
        iconEmoji = goal.iconEmoji,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun BarrierOptionCard(
    barrier: BarrierOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableOptionCard(
        title = barrier.title,
        subtitle = barrier.subtitle,
        iconEmoji = barrier.iconEmoji,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun CommitmentOptionCard(
    option: CommitmentOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(if (isSelected) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.3f))
    val bgColor by animateColorAsState(if (isSelected) AppTheme.colors.primary.copy(alpha = 0.08f) else AppTheme.colors.surfaceContainer)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = option.title, style = AppTheme.typography.h5.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.text.primary)
                    if (option.tag != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (option.tag == "Recommended") AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.2f))
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
                Text(text = option.subtitle, style = AppTheme.typography.bodySmall, color = AppTheme.colors.text.secondary)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(width = 2.dp, color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.5f), shape = CircleShape)
                    .background(if (isSelected) AppTheme.colors.primary else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(painter = painterResource(UiRes.drawable.ic_check), contentDescription = null, tint = AppTheme.colors.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun SelectableOptionCard(
    title: String,
    subtitle: String,
    iconEmoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(if (isSelected) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.3f))
    val bgColor by animateColorAsState(if (isSelected) AppTheme.colors.primary.copy(alpha = 0.08f) else AppTheme.colors.surfaceContainer)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(AppTheme.colors.outline.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = iconEmoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = AppTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.text.primary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = AppTheme.typography.bodySmall, color = AppTheme.colors.text.secondary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(width = 2.dp, color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.5f), shape = CircleShape)
                    .background(if (isSelected) AppTheme.colors.primary else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(painter = painterResource(UiRes.drawable.ic_check), contentDescription = null, tint = AppTheme.colors.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
