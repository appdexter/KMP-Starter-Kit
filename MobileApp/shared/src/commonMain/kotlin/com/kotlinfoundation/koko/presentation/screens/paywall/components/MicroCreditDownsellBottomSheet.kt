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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.kotlinfoundation.koko.designsystem.components.AppButton
import com.kotlinfoundation.koko.designsystem.components.ButtonSize
import com.kotlinfoundation.koko.designsystem.components.ButtonStyle
import com.kotlinfoundation.koko.designsystem.generated.resources.UiRes
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_check
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_coin_credits
import com.kotlinfoundation.koko.designsystem.theme.AppTheme
import com.kotlinfoundation.koko.generated.resources.Res
import com.kotlinfoundation.koko.generated.resources.downsell_credit_cta
import com.kotlinfoundation.koko.generated.resources.downsell_credit_decline
import com.kotlinfoundation.koko.generated.resources.downsell_credit_perk_1
import com.kotlinfoundation.koko.generated.resources.downsell_credit_perk_2
import com.kotlinfoundation.koko.generated.resources.downsell_credit_perk_3
import com.kotlinfoundation.koko.generated.resources.downsell_credit_subtitle
import com.kotlinfoundation.koko.generated.resources.downsell_credit_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom Sheet modal offering a one-time micro credit pack as a downsell
 * for users hesitant to commit to a recurring subscription.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicroCreditDownsellBottomSheet(
    creditsAmount: Int,
    priceText: String,
    onBuyCredits: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = AppTheme.colors.surfaceContainer,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppTheme.colors.outline.copy(alpha = 0.35f)),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
    ) {
        MicroCreditDownsellContent(
            creditsAmount = creditsAmount,
            priceText = priceText,
            onBuyCredits = onBuyCredits,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

/**
 * Pure content composable for Micro Credit Downsell, suitable for standalone preview and testing.
 */
@Composable
fun MicroCreditDownsellContent(
    creditsAmount: Int,
    priceText: String,
    onBuyCredits: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppTheme.spacing.outerSpacing,
                end = AppTheme.spacing.outerSpacing,
                bottom = AppTheme.spacing.outerSpacing + 8.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.groupedVerticalElementSpacing),
    ) {
        // Coin Icon Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppTheme.colors.primary.copy(alpha = 0.22f),
                            AppTheme.colors.primary.copy(alpha = 0.05f),
                        ),
                    ),
                )
                .border(
                    width = 1.5.dp,
                    color = AppTheme.colors.primary.copy(alpha = 0.35f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(UiRes.drawable.ic_coin_credits),
                contentDescription = null,
                tint = AppTheme.colors.primary,
                modifier = Modifier.size(38.dp),
            )
        }

        // Title
        Text(
            text = stringResource(Res.string.downsell_credit_title),
            style = AppTheme.typography.h4,
            color = AppTheme.colors.text.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Subtitle
        Text(
            text = stringResource(Res.string.downsell_credit_subtitle, creditsAmount, priceText),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Value Props Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppTheme.colors.primary.copy(alpha = 0.05f))
                .border(
                    width = 1.dp,
                    color = AppTheme.colors.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(AppTheme.spacing.cardContentSpacing),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DownsellPerkRow(text = stringResource(Res.string.downsell_credit_perk_1))
            DownsellPerkRow(text = stringResource(Res.string.downsell_credit_perk_2))
            DownsellPerkRow(text = stringResource(Res.string.downsell_credit_perk_3, creditsAmount))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // CTA Button
        AppButton(
            text = stringResource(Res.string.downsell_credit_cta, creditsAmount, priceText),
            style = ButtonStyle.PRIMARY,
            size = ButtonSize.LARGE,
            onClick = onBuyCredits,
            modifier = Modifier.fillMaxWidth(),
        )

        // Secondary Text Button
        AppButton(
            text = stringResource(Res.string.downsell_credit_decline),
            style = ButtonStyle.TEXT,
            size = ButtonSize.MEDIUM,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DownsellPerkRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(UiRes.drawable.ic_check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = text,
            style = AppTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = AppTheme.colors.text.primary,
        )
    }
}

@Preview
@Composable
private fun MicroCreditDownsellContentPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MicroCreditDownsellContent(
                creditsAmount = 50,
                priceText = "$4.99",
                onBuyCredits = {},
                onDismiss = {},
            )
        }
    }
}
