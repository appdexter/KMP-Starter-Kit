package com.kotlinfoundation.koko.presentation.screens.paywall.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kotlinfoundation.koko.designsystem.components.AppButton
import com.kotlinfoundation.koko.designsystem.components.ButtonSize
import com.kotlinfoundation.koko.designsystem.components.ButtonStyle
import com.kotlinfoundation.koko.designsystem.generated.resources.UiRes
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_close
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_crown
import com.kotlinfoundation.koko.designsystem.theme.AppTheme
import com.kotlinfoundation.koko.generated.resources.Res
import com.kotlinfoundation.koko.generated.resources.downsell_exit_badge
import com.kotlinfoundation.koko.generated.resources.downsell_exit_cta
import com.kotlinfoundation.koko.generated.resources.downsell_exit_decline
import com.kotlinfoundation.koko.generated.resources.downsell_exit_headline
import com.kotlinfoundation.koko.generated.resources.downsell_exit_regular_price
import com.kotlinfoundation.koko.generated.resources.downsell_exit_special_price
import com.kotlinfoundation.koko.generated.resources.downsell_exit_subtitle
import com.kotlinfoundation.koko.generated.resources.downsell_exit_timer_expires
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Exit intent downsell dialog displayed when a user attempts to dismiss the paywall.
 * Features an exclusive discount badge, countdown urgency timer, strikethrough price comparison,
 * and high-converting CTA buttons.
 */
@Composable
fun ExitIntentDownsellDialog(
    discountPercent: Int,
    timeRemainingSeconds: Int,
    discountedPrice: String?,
    originalPrice: String?,
    onClaimDiscount: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.outerSpacing),
            contentAlignment = Alignment.Center,
        ) {
            ExitIntentDownsellCard(
                discountPercent = discountPercent,
                timeRemainingSeconds = timeRemainingSeconds,
                discountedPrice = discountedPrice,
                originalPrice = originalPrice,
                onClaimDiscount = onClaimDiscount,
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }
    }
}

/**
 * Pure card composable for Exit Intent Downsell, suitable for standalone preview and testing.
 */
@Composable
fun ExitIntentDownsellCard(
    discountPercent: Int,
    timeRemainingSeconds: Int,
    discountedPrice: String?,
    originalPrice: String?,
    onClaimDiscount: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeSeconds = timeRemainingSeconds.coerceAtLeast(0)
    val minutes = (safeSeconds / 60).toString().padStart(2, '0')
    val seconds = (safeSeconds % 60).toString().padStart(2, '0')
    val formattedCountdown = "$minutes:$seconds"

    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.surfaceContainer)
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colors.primary.copy(alpha = 0.6f),
                        AppTheme.colors.primary.copy(alpha = 0.15f),
                    ),
                ),
                shape = shape,
            )
            .padding(AppTheme.spacing.cardContentSpacing),
    ) {
        // Top-right close button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.outline.copy(alpha = 0.15f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(UiRes.drawable.ic_close),
                contentDescription = "Close",
                tint = AppTheme.colors.text.secondary,
                modifier = Modifier.size(16.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.groupedVerticalElementSpacing),
        ) {
            // Header: Discount Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AppTheme.colors.primary,
                                AppTheme.colors.primary.copy(alpha = 0.85f),
                            ),
                        ),
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(UiRes.drawable.ic_crown),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(Res.string.downsell_exit_badge, discountPercent),
                        style = AppTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp,
                        ),
                        color = Color.White,
                    )
                }
            }

            // Headline
            Text(
                text = stringResource(Res.string.downsell_exit_headline),
                style = AppTheme.typography.h4,
                color = AppTheme.colors.text.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            // Subtitle
            Text(
                text = stringResource(Res.string.downsell_exit_subtitle),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            // Countdown Timer Card
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.status.errorContainer.copy(alpha = 0.6f))
                    .border(
                        width = 1.dp,
                        color = AppTheme.colors.status.error.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "⏳", fontSize = 16.sp)
                Text(
                    text = stringResource(Res.string.downsell_exit_timer_expires),
                    style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = AppTheme.colors.status.error,
                )
                Text(
                    text = formattedCountdown,
                    style = AppTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                    color = AppTheme.colors.status.error,
                )
            }

            // Price Comparison Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.primary.copy(alpha = 0.06f))
                    .border(
                        width = 1.dp,
                        color = AppTheme.colors.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Original price
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(Res.string.downsell_exit_regular_price),
                            style = AppTheme.typography.bodyExtraSmall,
                            color = AppTheme.colors.text.secondary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = originalPrice ?: "$59.99/yr",
                            style = AppTheme.typography.bodyLarge.copy(
                                textDecoration = TextDecoration.LineThrough,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = AppTheme.colors.text.secondary,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(AppTheme.colors.outline.copy(alpha = 0.3f)),
                    )

                    // Discounted price
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(Res.string.downsell_exit_special_price),
                            style = AppTheme.typography.bodyExtraSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = AppTheme.colors.primary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = discountedPrice ?: "$35.99/yr",
                            style = AppTheme.typography.h4.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = AppTheme.colors.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppButton(
                    text = stringResource(Res.string.downsell_exit_cta, discountPercent),
                    style = ButtonStyle.PRIMARY,
                    size = ButtonSize.LARGE,
                    onClick = onClaimDiscount,
                    modifier = Modifier.fillMaxWidth(),
                )

                AppButton(
                    text = stringResource(Res.string.downsell_exit_decline),
                    style = ButtonStyle.TEXT,
                    size = ButtonSize.SMALL,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ExitIntentDownsellCardPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ExitIntentDownsellCard(
                discountPercent = 40,
                timeRemainingSeconds = 599,
                discountedPrice = "$35.99/yr",
                originalPrice = "$59.99/yr",
                onClaimDiscount = {},
                onDismiss = {},
            )
        }
    }
}
