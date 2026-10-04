package com.kotlinfoundation.koko.presentation.screens.profile

import com.kotlinfoundation.koko.domain.model.User

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val deleteUserDialogShown: Boolean = false,
    val errorMessage: String? = null,
    val isDevModeUnlocked: Boolean = false,
    val appVersionInfo: String = "1.0.0",
    val navigateToDebugMenu: Boolean = false,
    val feedbackMessage: String? = null,
    val canUpgradeToPremium: Boolean = false,
) {
    val signInActionRequired: Boolean get() = user == null && isLoading.not()
}

sealed interface ProfileScreenUiEvent {
    data object OnClickDeleteAccount : ProfileScreenUiEvent
    data object OnVersionTapped : ProfileScreenUiEvent
    data object OnDebugMenuNavigated : ProfileScreenUiEvent
    data object OnDismissFeedback : ProfileScreenUiEvent
}
