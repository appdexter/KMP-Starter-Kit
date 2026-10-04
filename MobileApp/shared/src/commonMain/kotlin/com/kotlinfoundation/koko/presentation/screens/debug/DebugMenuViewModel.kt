package com.kotlinfoundation.koko.presentation.screens.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.devtools.DiagnosticsHelper
import com.kotlinfoundation.koko.growth.experiment.ExperimentEngine
import com.kotlinfoundation.koko.identity.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DebugMenuViewModel(
    private val sessionManager: SessionManager,
    private val experimentEngine: ExperimentEngine,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DebugMenuUiState(
            appVersion = "1.0.0",
            environment = "DEV",
            anonymousId = sessionManager.getAnonymousId(),
            firebaseUid = sessionManager.userIdentity.value.firebaseUid,
            isForcePremium = sessionManager.userIdentity.value.isPremium,
            diagnosticsJson = DiagnosticsHelper.generateDiagnosticsReport(
                sessionManager = sessionManager,
                environment = "DEV",
                appVersion = "1.0.0",
            ),
        ),
    )
    val uiState: StateFlow<DebugMenuUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.userIdentity.collect { identity ->
                _uiState.update { current ->
                    current.copy(
                        anonymousId = identity.anonymousId,
                        firebaseUid = identity.firebaseUid,
                        isForcePremium = identity.isPremium,
                    )
                }
                refreshDiagnosticsInternal()
            }
        }
    }

    fun onUiEvent(event: DebugMenuUiEvent) {
        when (event) {
            is DebugMenuUiEvent.SetForcePremium -> {
                sessionManager.setPremium(event.enabled)
                _uiState.update {
                    it.copy(
                        isForcePremium = event.enabled,
                        message = "Force Premium set to ${event.enabled}",
                    )
                }
                refreshDiagnosticsInternal()
            }

            is DebugMenuUiEvent.OverrideExperiment -> {
                viewModelScope.launch {
                    experimentEngine.overrideVariant(event.experimentId, event.variant)
                    _uiState.update {
                        it.copy(
                            message = "Experiment ${event.experimentId} set to ${event.variant ?: "default"}",
                        )
                    }
                }
            }

            is DebugMenuUiEvent.ResetOnboarding -> {
                viewModelScope.launch {
                    userPreferences.putBoolean(UserPreferences.Keys.KEY_FIRST_TIME_USER, true)
                    userPreferences.putBoolean(UserPreferences.Keys.KEY_IS_ONBOARD_SHOWN, false)
                    _uiState.update {
                        it.copy(message = "Onboarding reset successfully")
                    }
                }
            }

            is DebugMenuUiEvent.RefreshDiagnostics -> {
                refreshDiagnosticsInternal()
                _uiState.update {
                    it.copy(message = "Diagnostics refreshed")
                }
            }

            is DebugMenuUiEvent.DismissMessage -> {
                _uiState.update { it.copy(message = null) }
            }
        }
    }

    private fun refreshDiagnosticsInternal() {
        val current = _uiState.value
        val report = DiagnosticsHelper.generateDiagnosticsReport(
            sessionManager = sessionManager,
            environment = current.environment,
            appVersion = current.appVersion,
        )
        _uiState.update { it.copy(diagnosticsJson = report) }
    }
}
