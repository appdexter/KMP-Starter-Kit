package com.kotlinfoundation.koko.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kotlinfoundation.koko.designsystem.components.AppButton
import com.kotlinfoundation.koko.designsystem.components.ButtonSize
import com.kotlinfoundation.koko.designsystem.components.ButtonStyle
import com.kotlinfoundation.koko.designsystem.components.LoadingProgress
import com.kotlinfoundation.koko.designsystem.components.LoadingProgressMode
import com.kotlinfoundation.koko.designsystem.components.ScreenWithToolbar
import com.kotlinfoundation.koko.designsystem.components.SettingItemListContainer
import com.kotlinfoundation.koko.designsystem.components.SettingsItemUiState
import com.kotlinfoundation.koko.designsystem.components.UserInput
import com.kotlinfoundation.koko.designsystem.components.modals.AppDialog
import com.kotlinfoundation.koko.designsystem.components.modals.DeleteUserConfirmation
import com.kotlinfoundation.koko.designsystem.components.modals.DialogType
import com.kotlinfoundation.koko.designsystem.components.premium.UpgradePremiumBanner
import com.kotlinfoundation.koko.designsystem.components.premium.UpgradePremiumBannerStyle
import com.kotlinfoundation.koko.designsystem.generated.resources.UiRes
import com.kotlinfoundation.koko.designsystem.generated.resources.btn_delete_account
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_back
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_delete
import com.kotlinfoundation.koko.designsystem.generated.resources.ic_profile_img_placeholder
import com.kotlinfoundation.koko.designsystem.theme.AppTheme
import com.kotlinfoundation.koko.domain.model.User
import com.kotlinfoundation.koko.generated.resources.Res
import com.kotlinfoundation.koko.generated.resources.btn_upgrade_premium
import com.kotlinfoundation.koko.generated.resources.title_screen_profile
import com.kotlinfoundation.koko.root.AppGlobalUiState
import com.kotlinfoundation.koko.util.UiMessage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    onSignInRequired: () -> Unit,
    onNavigateToBack: () -> Unit,
    onNavigateToDebugMenu: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.signInActionRequired) {
        if (uiState.signInActionRequired) {
            onSignInRequired()
        }
    }

    LaunchedEffect(uiState.navigateToDebugMenu) {
        if (uiState.navigateToDebugMenu) {
            onNavigateToDebugMenu()
            viewModel.onUiEvent(ProfileScreenUiEvent.OnDebugMenuNavigated)
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            AppGlobalUiState.showUiMessage(UiMessage.Message(it))
            viewModel.onUiEvent(ProfileScreenUiEvent.OnDismissFeedback)
        }
    }

    if (uiState.deleteUserDialogShown) {
        DeleteUserConfirmation(
            onConfirm = viewModel::onConfirmDeleteAccount,
            onDismiss = viewModel::onDismissDeleteUserConfirmationDialog,
        )
    }

    if (uiState.errorMessage.isNullOrEmpty().not()) {
        AppDialog(
            type = DialogType.ERROR,
            text = uiState.errorMessage,
            onConfirm = { viewModel.onErrorMessageShown() },
        )
    }
    if (uiState.isLoading) {
        LoadingProgress(mode = LoadingProgressMode.FULLSCREEN)
    } else {
        val currentUser = uiState.user ?: User(id = "guest", displayName = "Guest User")
        ScreenWithToolbar(
            modifier = modifier.fillMaxSize().background(AppTheme.colors.background),
            title = stringResource(Res.string.title_screen_profile),
            navigationIcon = UiRes.drawable.ic_back,
            onNavigationIconClick = onNavigateToBack,
            isScrollableContent = true,
            includeBottomInsets = true,
        ) {
            ProfileScreen(
                modifier = Modifier.fillMaxSize(),
                currentUser = currentUser,
                uiState = uiState,
                onUiEvent = viewModel::onUiEvent,
                onNavigateToDebugMenu = onNavigateToDebugMenu,
                onNavigateToPaywall = onNavigateToPaywall,
            )
        }
    }
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    currentUser: User,
    uiState: ProfileUiState = ProfileUiState(),
    onUiEvent: (ProfileScreenUiEvent) -> Unit,
    onNavigateToDebugMenu: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sectionSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Profile Picture
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(currentUser.photoUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(UiRes.drawable.ic_profile_img_placeholder),
            error = painterResource(UiRes.drawable.ic_profile_img_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(100.dp).clip(CircleShape),
        )

        // Full Name
        UserInputWithLabel(label = "Display Name") {
            UserInput(
                value = currentUser.displayName ?: "",
                readOnly = true,
                onValueChange = {},
            )
        }

        // Email
        UserInputWithLabel(label = "Email") {
            UserInput(
                value = currentUser.email ?: "",
                readOnly = true,
                onValueChange = {},
            )
        }

        // Upgrade to Premium
        if (uiState.canUpgradeToPremium) {
            UpgradePremiumBanner(
                modifier = Modifier.fillMaxWidth(),
                style = UpgradePremiumBannerStyle.LARGE,
                onClick = onNavigateToPaywall,
            )
            AppButton(
                text = stringResource(Res.string.btn_upgrade_premium),
                style = ButtonStyle.PRIMARY,
                size = ButtonSize.LARGE,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToPaywall,
            )
        }

        SettingItemListContainer(
            onClick = { onUiEvent(ProfileScreenUiEvent.OnClickDeleteAccount) },
            itemTextStyle = AppTheme.typography.h5.copy(fontWeight = FontWeight.SemiBold),
            itemList = listOf(
                SettingsItemUiState(
                    textRes = UiRes.string.btn_delete_account,
                    startIcon = UiRes.drawable.ic_delete,
                    showEndIcon = false,
                    textIconColor = AppTheme.colors.status.error,
                ),
            ),
        )

        // App Version (Tap 5 consecutive times to unlock Dev Mode)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    onUiEvent(ProfileScreenUiEvent.OnVersionTapped)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Version ${uiState.appVersionInfo.ifBlank { "1.0.0" }}",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.text.secondary,
            )
            if (uiState.isDevModeUnlocked) {
                Text(
                    text = "Developer Mode Active",
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Developer Debug Menu Button (only visible when unlocked)
        if (uiState.isDevModeUnlocked) {
            AppButton(
                text = "Developer Debug Menu",
                style = ButtonStyle.ALTERNATIVE,
                size = ButtonSize.SMALL,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToDebugMenu,
            )
        }
    }
}

@Composable
fun UserInputWithLabel(
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    userInput: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.defaultSpacing),
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyExtraLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.text.primary,
        )
        userInput()
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    AppTheme {
        ProfileScreen(
            currentUser = User(id = "1", displayName = "Jane Doe", email = "jane@example.com"),
            uiState = ProfileUiState(isDevModeUnlocked = true, appVersionInfo = "1.0.0", canUpgradeToPremium = true),
            onUiEvent = {},
        )
    }
}
