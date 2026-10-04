package com.kotlinfoundation.koko.presentation.screens.debug

data class DebugMenuUiState(
    val appVersion: String = "1.0.0",
    val environment: String = "DEV",
    val anonymousId: String = "",
    val firebaseUid: String? = null,
    val isForcePremium: Boolean = false,
    val diagnosticsJson: String = "",
    val message: String? = null,
)

sealed interface DebugMenuUiEvent {
    data class SetForcePremium(val enabled: Boolean) : DebugMenuUiEvent
    data class OverrideExperiment(val experimentId: String, val variant: String?) : DebugMenuUiEvent
    data object ResetOnboarding : DebugMenuUiEvent
    data object RefreshDiagnostics : DebugMenuUiEvent
    data object DismissMessage : DebugMenuUiEvent
}
