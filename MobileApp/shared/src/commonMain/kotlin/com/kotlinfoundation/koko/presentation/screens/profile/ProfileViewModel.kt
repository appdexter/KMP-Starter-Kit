package com.kotlinfoundation.koko.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinfoundation.koko.data.repository.SubscriptionRepository
import com.kotlinfoundation.koko.data.repository.UserRepository
import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.domain.exceptions.UnAuthorizedException
import com.kotlinfoundation.koko.domain.model.User
import com.kotlinfoundation.koko.domain.model.isFree
import com.kotlinfoundation.koko.root.AppConfiguration
import com.kotlinfoundation.koko.util.AppUtil
import com.kotlinfoundation.koko.util.analytics.Analytics
import com.kotlinfoundation.koko.util.analytics.NoImplAnalytics
import com.kotlinfoundation.koko.util.logging.AppLogger
import com.mmk.kmpauth.core.auth.KMPAuthRecentLoginRequiredException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

class ProfileViewModel private constructor(
    private val userRepository: UserRepository?,
    private val userPreferences: UserPreferences,
    private val appUtil: AppUtil,
    private val subscriptionRepository: SubscriptionRepository?,
    currentUserFlowOverride: Flow<Result<User>>?,
    private val deleteAccountAction: (suspend () -> Result<Unit>)?,
    private val analytics: Analytics,
) : ViewModel() {

    constructor(
        userRepository: UserRepository,
        userPreferences: UserPreferences,
        appUtil: AppUtil,
        subscriptionRepository: SubscriptionRepository,
        analytics: Analytics = NoImplAnalytics,
    ) : this(
        userRepository = userRepository,
        userPreferences = userPreferences,
        appUtil = appUtil,
        subscriptionRepository = subscriptionRepository,
        currentUserFlowOverride = null,
        deleteAccountAction = null,
        analytics = analytics,
    )

    internal constructor(
        userPreferences: UserPreferences,
        appUtil: AppUtil,
        currentUserFlowOverride: Flow<Result<User>>,
        deleteAccountAction: (suspend () -> Result<Unit>)? = null,
        subscriptionRepository: SubscriptionRepository? = null,
        analytics: Analytics = NoImplAnalytics,
    ) : this(
        userRepository = null,
        userPreferences = userPreferences,
        appUtil = appUtil,
        subscriptionRepository = subscriptionRepository,
        currentUserFlowOverride = currentUserFlowOverride,
        deleteAccountAction = deleteAccountAction,
        analytics = analytics,
    )

    private var lastTapTimeMark = TimeSource.Monotonic.markNow()
    private var versionTapCount = 0

    private val currentUserFlow = currentUserFlowOverride ?: userRepository?.currentUser ?: emptyFlow()

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            currentUserFlow.collect { result ->
                result.onSuccess { user ->
                    _uiState.update { it.copy(isLoading = false, user = user) }
                }.onFailure { error ->
                    if (error is UnAuthorizedException) {
                        _uiState.update { it.copy(isLoading = false, user = null) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                    }
                }
            }
        }

        viewModelScope.launch {
            val isUnlocked = userPreferences.getBoolean(UserPreferences.Keys.KEY_IS_DEV_MODE_ENABLED, false)
            val versionInfo = try {
                appUtil.getAppVersionInfo()
            } catch (_: Throwable) {
                "1.0.0"
            }
            _uiState.update {
                it.copy(
                    isDevModeUnlocked = isUnlocked,
                    appVersionInfo = versionInfo,
                )
            }
        }

        subscriptionRepository?.let { repo ->
            viewModelScope.launch {
                repo.currentSubscriptionFlow.collect { subscription ->
                    _uiState.update {
                        it.copy(
                            canUpgradeToPremium = AppConfiguration.PREMIUM_FEATURES_ENABLED && subscription.isFree,
                        )
                    }
                }
            }
        } ?: run {
            _uiState.update {
                it.copy(canUpgradeToPremium = AppConfiguration.PREMIUM_FEATURES_ENABLED)
            }
        }
    }

    fun onErrorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onDismissDeleteUserConfirmationDialog() = viewModelScope.launch {
        _uiState.update { it.copy(deleteUserDialogShown = false) }
    }

    fun onConfirmDeleteAccount() = viewModelScope.launch {
        analytics.logEvent("delete_account_confirmed")
        _uiState.update { it.copy(deleteUserDialogShown = false, isLoading = true) }
        val result = deleteAccountAction?.invoke() ?: userRepository?.deleteAccount() ?: Result.success(Unit)
        result.onSuccess {
            AppLogger.d("Account is deleted successfully")
            _uiState.update { it.copy(isLoading = false, user = null) }
        }.onFailure { error ->
            AppLogger.d("Account deletion failed ${error.message}")
            if (error is KMPAuthRecentLoginRequiredException) {
                _uiState.update { it.copy(isLoading = false, user = null) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    fun onUiEvent(event: ProfileScreenUiEvent) = viewModelScope.launch {
        when (event) {
            ProfileScreenUiEvent.OnClickDeleteAccount -> {
                analytics.logEvent("delete_account_requested")
                _uiState.update { it.copy(deleteUserDialogShown = true) }
            }

            ProfileScreenUiEvent.OnVersionTapped -> {
                if (_uiState.value.isDevModeUnlocked) {
                    _uiState.update { it.copy(navigateToDebugMenu = true) }
                    return@launch
                }

                val elapsed = lastTapTimeMark.elapsedNow().inWholeMilliseconds
                lastTapTimeMark = TimeSource.Monotonic.markNow()

                if (elapsed > 2000L) {
                    versionTapCount = 1
                } else {
                    versionTapCount++
                }

                if (versionTapCount >= 5) {
                    analytics.logEvent("dev_mode_unlocked")
                    userPreferences.putBoolean(UserPreferences.Keys.KEY_IS_DEV_MODE_ENABLED, true)
                    versionTapCount = 0
                    _uiState.update {
                        it.copy(
                            isDevModeUnlocked = true,
                            navigateToDebugMenu = true,
                            feedbackMessage = "Developer mode unlocked! 🚀",
                        )
                    }
                } else if (versionTapCount >= 2) {
                    val remaining = 5 - versionTapCount
                    _uiState.update {
                        it.copy(feedbackMessage = "Tap $remaining more times to unlock Developer Menu")
                    }
                }
            }

            ProfileScreenUiEvent.OnDebugMenuNavigated -> {
                _uiState.update { it.copy(navigateToDebugMenu = false) }
            }

            ProfileScreenUiEvent.OnDismissFeedback -> {
                _uiState.update { it.copy(feedbackMessage = null) }
            }
        }
    }
}
