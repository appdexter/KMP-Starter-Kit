package com.kotlinfoundation.koko.presentation.screens.onboarding.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlinfoundation.koko.designsystem.components.AppButton
import com.kotlinfoundation.koko.designsystem.components.ButtonSize
import com.kotlinfoundation.koko.designsystem.components.ButtonStyle
import com.kotlinfoundation.koko.designsystem.generated.resources.UiRes
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_check
import com.kotlinfoundation.koko.designsystem.theme.AppTheme
import com.kotlinfoundation.koko.presentation.screens.onboarding.TestimonialItem
import org.jetbrains.compose.resources.painterResource

@Composable
fun SocialProofStep(
    testimonials: List<TestimonialItem>,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Backed by Behavioral Science", style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = AppTheme.colors.text.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Join thousands of members who unlocked peak focus and daily momentum.", style = AppTheme.typography.bodyMedium, color = AppTheme.colors.text.secondary)
        Spacer(modifier = Modifier.height(20.dp))

        // Big Stat Banner
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(AppTheme.colors.primary.copy(alpha = 0.15f), AppTheme.colors.primary.copy(alpha = 0.05f))))
                .border(1.dp, AppTheme.colors.primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "87%", style = AppTheme.typography.h1.copy(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold), color = AppTheme.colors.primary)
                Text(
                    text = "of members achieve their target habit in under 30 days",
                    style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.text.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "★★★★★", style = AppTheme.typography.bodyMedium.copy(fontSize = 16.sp), color = Color(0xFFFFB800))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "4.9 / 5.0 (12,400+ reviews)", style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = AppTheme.colors.text.secondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            testimonials.forEach { item -> TestimonialCard(item = item) }
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

@Composable
fun TestimonialCard(
    item: TestimonialItem,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surfaceContainer)
            .border(1.dp, AppTheme.colors.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = item.name, style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.text.primary)
                    Text(text = item.role, style = AppTheme.typography.bodySmall, color = AppTheme.colors.text.secondary)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(AppTheme.colors.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(text = item.highlight, style = AppTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = AppTheme.colors.primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "\"${item.quote}\"", style = AppTheme.typography.bodySmall.copy(lineHeight = 18.sp), color = AppTheme.colors.text.secondary)
        }
    }
}

@Composable
fun LaborIllusionStep(
    progress: Float,
    statusText: String,
    dailyMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    )

    Column(
        modifier = modifier.fillMaxSize().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Personalizing Your Plan...", style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = AppTheme.colors.text.primary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Analyzing your profile inputs with our behavioral engine", style = AppTheme.typography.bodyMedium, color = AppTheme.colors.text.secondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(36.dp))

        // Center Animated Progress Circle
        Box(modifier = Modifier.size(160.dp).scale(pulseScale), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(160.dp), color = AppTheme.colors.outline.copy(alpha = 0.2f), strokeWidth = 10.dp)
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(160.dp), color = AppTheme.colors.primary, strokeWidth = 10.dp, strokeCap = StrokeCap.Round)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${(progress * 100).toInt()}%", style = AppTheme.typography.h2.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold), color = AppTheme.colors.text.primary)
                Text(text = "Optimizing", style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = AppTheme.colors.text.secondary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = statusText,
            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChecklistRow(label = "Analyzing friction points & primary goal", isCompleted = progress > 0.30f)
            ChecklistRow(label = "Calibrating $dailyMinutes min/day micro-schedule", isCompleted = progress > 0.65f)
            ChecklistRow(label = "Synthesizing 90-day compounding roadmap", isCompleted = progress > 0.90f)
        }
    }
}

@Composable
fun ChecklistRow(
    label: String,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(if (isCompleted) AppTheme.colors.primary else AppTheme.colors.outline.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted) {
                Icon(painter = painterResource(UiRes.drawable.ic_check), contentDescription = null, tint = AppTheme.colors.onPrimary, modifier = Modifier.size(13.dp))
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

@Composable
fun LossAversionStep(
    dailyMinutes: Int,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("The Cost of Inconsistency", style = AppTheme.typography.h3.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = AppTheme.colors.text.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Small daily habits compound over 90 days. Here is what your trajectory looks like.", style = AppTheme.typography.bodyMedium, color = AppTheme.colors.text.secondary)
        Spacer(modifier = Modifier.height(20.dp))

        TrajectoryComparisonChart(modifier = Modifier.fillMaxWidth().height(180.dp))

        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Negative Trajectory (Status Quo)
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFE5E5).copy(alpha = 0.35f))
                    .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "❌", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Status Quo (Without Structured Plan)", style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD32F2F))
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
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.primary.copy(alpha = 0.08f))
                    .border(1.5.dp, AppTheme.colors.primary, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🚀", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "With Your $dailyMinutes-Min Daily Routine", style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.primary)
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
fun TrajectoryComparisonChart(modifier: Modifier = Modifier) {
    val primaryColor = AppTheme.colors.primary
    val negativeColor = Color(0xFFFF5252)

    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surfaceContainer)
            .border(1.dp, AppTheme.colors.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val gridColor = Color.Gray.copy(alpha = 0.15f)
            drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(0f, height * 0.50f), Offset(width, height * 0.50f), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = 1.dp.toPx())

            val pathStatusQuo = Path().apply {
                moveTo(0f, height * 0.70f)
                cubicTo(width * 0.25f, height * 0.65f, width * 0.50f, height * 0.85f, width, height * 0.88f)
            }
            drawPath(pathStatusQuo, color = negativeColor, style = Stroke(2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)))

            val pathSuccess = Path().apply {
                moveTo(0f, height * 0.70f)
                cubicTo(width * 0.35f, height * 0.68f, width * 0.65f, height * 0.35f, width, height * 0.12f)
            }
            drawPath(pathSuccess, color = primaryColor, style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(width, height * 0.12f))
        }

        // Floating Legend Labels
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Day 1", style = AppTheme.typography.bodySmall.copy(fontSize = 10.sp), color = AppTheme.colors.text.secondary)
            Text(text = "Day 30", style = AppTheme.typography.bodySmall.copy(fontSize = 10.sp), color = AppTheme.colors.text.secondary)
            Text(text = "Day 90 (10x Output)", style = AppTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = primaryColor)
        }
    }
}
