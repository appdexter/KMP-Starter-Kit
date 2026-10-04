package com.kotlinfoundation.koko.presentation.screens.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinfoundation.koko.designsystem.components.AppButton
import com.kotlinfoundation.koko.designsystem.components.ButtonSize
import com.kotlinfoundation.koko.designsystem.components.ButtonStyle
import com.kotlinfoundation.koko.designsystem.components.ScreenWithToolbar
import com.kotlinfoundation.koko.designsystem.generated.resources.UiRes
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_back
import com.kotlinfoundation.koko.designsystem.theme.AppTheme

@Composable
fun DebugMenuScreen(
    viewModel: DebugMenuViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    DebugMenuScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        onNavigateBack = onNavigateBack,
        onCopyText = { text ->
            clipboardManager.setText(AnnotatedString(text))
        },
    )
}

@Composable
fun DebugMenuScreen(
    uiState: DebugMenuUiState,
    onUiEvent: (DebugMenuUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onCopyText: (String) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            onUiEvent(DebugMenuUiEvent.DismissMessage)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenWithToolbar(
            modifier = Modifier.fillMaxSize(),
            title = "Dev Diagnostics & Debug",
            includeBottomInsets = true,
            isScrollableContent = true,
            onNavigationIconClick = onNavigateBack,
            navigationIcon = UiRes.drawable.ic_back,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Section 1: System Info
                DebugSectionCard(title = "1. System Information") {
                    DebugInfoRow(label = "Environment", value = uiState.environment)
                    DebugInfoRow(label = "App Version", value = uiState.appVersion)
                    DebugInfoRow(label = "Anonymous ID", value = uiState.anonymousId)
                    DebugInfoRow(
                        label = "Firebase UID",
                        value = uiState.firebaseUid ?: "Not Authenticated (Anonymous)",
                    )
                }

                // Section 2: Overrides
                DebugSectionCard(title = "2. Dev Overrides") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Force Premium / Mock VIP",
                                style = AppTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = AppTheme.colors.text.primary,
                            )
                            Text(
                                text = "Bypasses paywall and unlocks premium features",
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.text.secondary,
                            )
                        }
                        Switch(
                            checked = uiState.isForcePremium,
                            onCheckedChange = { onUiEvent(DebugMenuUiEvent.SetForcePremium(it)) },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AppButton(
                        text = "Reset Onboarding State",
                        style = ButtonStyle.ALTERNATIVE,
                        size = ButtonSize.SMALL,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onUiEvent(DebugMenuUiEvent.ResetOnboarding) },
                    )
                }

                // Section 3: Diagnostics JSON
                DebugSectionCard(title = "3. Diagnostics Report") {
                    val codeScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                color = AppTheme.colors.surfaceContainer,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .border(
                                width = 1.dp,
                                color = AppTheme.colors.outline,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(12.dp)
                            .horizontalScroll(codeScrollState),
                    ) {
                        Text(
                            text = uiState.diagnosticsJson.ifBlank { "No diagnostics data" },
                            style = AppTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            ),
                            color = AppTheme.colors.text.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppButton(
                            text = "Refresh",
                            style = ButtonStyle.ALTERNATIVE,
                            size = ButtonSize.SMALL,
                            modifier = Modifier.weight(1f),
                            onClick = { onUiEvent(DebugMenuUiEvent.RefreshDiagnostics) },
                        )
                        AppButton(
                            text = "Copy JSON",
                            style = ButtonStyle.PRIMARY,
                            size = ButtonSize.SMALL,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onCopyText(uiState.diagnosticsJson)
                                onUiEvent(DebugMenuUiEvent.RefreshDiagnostics)
                            },
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun DebugSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = AppTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
                color = AppTheme.colors.primary,
            )
            content()
        }
    }
}

@Composable
private fun DebugInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = AppTheme.colors.text.secondary,
        )
        Text(
            text = value,
            style = AppTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
            ),
            color = AppTheme.colors.text.primary,
        )
    }
}

@Preview
@Composable
private fun DebugMenuScreenPreview() {
    AppTheme {
        DebugMenuScreen(
            uiState = DebugMenuUiState(
                appVersion = "1.0.0",
                environment = "DEV",
                anonymousId = "anon-preview-12345",
                firebaseUid = "firebase-user-abcde",
                isForcePremium = true,
                diagnosticsJson = "{\n  \"app_name\": \"Koko\",\n  \"is_premium\": true\n}",
            ),
            onUiEvent = {},
            onNavigateBack = {},
            onCopyText = {},
        )
    }
}
