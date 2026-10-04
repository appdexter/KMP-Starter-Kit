package com.kotlinfoundation.koko.presentation.screens.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinfoundation.koko.data.repository.CreditRepository
import com.kotlinfoundation.koko.data.repository.SubscriptionRepository
import com.kotlinfoundation.koko.data.repository.UserRepository
import com.kotlinfoundation.koko.data.source.featureflag.FeatureFlagManager
import com.kotlinfoundation.koko.data.source.featureflag.getFunnelExperimentConfig
import com.kotlinfoundation.koko.data.source.featureflag.model.CloseButtonMode
import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.generated.resources.Res
import com.kotlinfoundation.koko.generated.resources.paywall_msg_credits_added
import com.kotlinfoundation.koko.generated.resources.paywall_msg_credits_not_added
import com.kotlinfoundation.koko.generated.resources.paywall_msg_sign_in_required
import com.kotlinfoundation.koko.root.AppGlobalUiState
import com.kotlinfoundation.koko.subscription.api.BillingPeriod
import com.kotlinfoundation.koko.subscription.api.PurchaseError
import com.kotlinfoundation.koko.subscription.api.PurchaseEventsListener
import com.kotlinfoundation.koko.subscription.api.PurchasePackage
import com.kotlinfoundation.koko.subscription.api.PurchasePackageId
import com.kotlinfoundation.koko.subscription.api.SubscriptionProviderUser
import com.kotlinfoundation.koko.util.Constants
import com.kotlinfoundation.koko.util.Constants.CREDIT_PACK_PRODUCT_ID_PREFIX
import com.kotlinfoundation.koko.util.UiMessage
import com.kotlinfoundation.koko.util.extensions.isCreditPackProductId
import com.kotlinfoundation.koko.util.extensions.parseCreditAmountFromProductId
import com.kotlinfoundation.koko.util.logging.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class PaywallViewModel(
    private val placementId: String?,
    private val subscriptionRepository: SubscriptionRepository,
    private val creditRepository: CreditRepository,
    private val userRepository: UserRepository,
    private val featureFlagManager: FeatureFlagManager,
    private val userPreferences: UserPreferences,
    private val mapper: PaywallUiStateMapper = PaywallUiStateMapper(),
    userGoal: String? = null,
    userBarrier: String? = null,
    userDailyMinutes: Int? = null,
) : ViewModel() {
    private val mode: PaywallMode =
        if (placementId == Constants.PAYWALL_PLACEMENT_CREDITS_PACK) {
            PaywallMode.CREDIT_PACK
        } else {
            PaywallMode.SUBSCRIPTION
        }

    private var rawPackages: List<PurchasePackage> = emptyList()
    private var selectedPackageId: PurchasePackageId? = null
    private var countdownJob: Job? = null

    private val _uiState = MutableStateFlow(
        PaywallUiState(
            mode = mode,
            currentPlacementId = placementId,
            isMock = subscriptionRepository.isMockProvider,
            userGoal = userGoal,
            userBarrier = userBarrier,
            userDailyMinutes = userDailyMinutes,
        ),
    )
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        val config = featureFlagManager.getFunnelExperimentConfig()

        _uiState.update {
            it.copy(
                downsellDiscountPercent = config.downsellLadder.tier2ExitIntent.discountPercent,
                downsellTimeRemainingSeconds = config.downsellLadder.tier2ExitIntent.countdownSeconds,
                microCreditAmount = config.downsellLadder.tier3MicroCredit.creditsAmount,
                microCreditPriceText = config.downsellLadder.tier3MicroCredit.priceText,
            )
        }

        when (config.primaryPaywall.closeButtonMode) {
            CloseButtonMode.IMMEDIATE -> {
                _uiState.update { it.copy(isCloseButtonVisible = true) }
            }

            CloseButtonMode.DELAYED -> {
                viewModelScope.launch {
                    delay(config.primaryPaywall.closeDelayMs)
                    _uiState.update { it.copy(isCloseButtonVisible = true) }
                }
            }

            CloseButtonMode.HIDDEN -> {
                _uiState.update { it.copy(isCloseButtonVisible = false) }
            }
        }

        viewModelScope.launch {
            val resolvedGoal = userGoal ?: userPreferences.getString(UserPreferences.KEY_USER_GOAL)
            val resolvedBarrier = userBarrier ?: userPreferences.getString(UserPreferences.KEY_USER_BARRIER)
            val resolvedMinutes = userDailyMinutes ?: userPreferences.getInt(UserPreferences.KEY_DAILY_COMMITMENT)
            _uiState.update {
                it.copy(
                    userGoal = resolvedGoal,
                    userBarrier = resolvedBarrier,
                    userDailyMinutes = resolvedMinutes,
                )
            }
        }

        if (featureFlagManager.getBoolean(FeatureFlagManager.Keys.SHOW_REMOTE_PAYWALL)) {
            // Native Adapty/RC paywall owns its own loading + package fetch.
            _uiState.update { it.copy(isLoading = false) }
        } else {
            fetchPackages()
        }
    }

    val remotePaywallPurchaseEventsListener: PurchaseEventsListener =
        object : PurchaseEventsListener {
            override fun onDismiss() {
                _uiState.update { it.copy(isDismissRequired = true) }
            }

            override fun onLoadingStateChanged(isLoading: Boolean) {
                _uiState.update { it.copy(isLoading = isLoading) }
            }

            override fun onUnknownError(error: Exception) {
                AppLogger.e("Unknown error occurred on paywall", error)
                _uiState.update {
                    it.copy(
                        errorMessage = UiMessage.Message(error.message),
                        isLoading = false,
                    )
                }
            }

            override fun onPurchaseSuccess(
                info: SubscriptionProviderUser,
                productIds: List<String>,
            ) {
                successfulPurchase(info, productIds)
            }

            override fun onRestoreSuccess(info: SubscriptionProviderUser) {
                successfulRestore(info)
            }

            override fun onPurchaseFailure(error: PurchaseError) {
                failedPurchase(error = Throwable(error.message))
            }

            override fun onRestoreFailure(error: PurchaseError) {
                failedRestore(error = Throwable(error.message))
            }
        }

    fun onMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onSignInActionHandled() = viewModelScope.launch {
        _uiState.update { it.copy(signInActionRequired = false) }
    }

    fun onPaywallDismissActionHandled() {
        countdownJob?.cancel()
        subscriptionRepository.onPaywallDismissed()
        _uiState.update { it.copy(isDismissRequired = false) }
    }

    fun onUiEvent(event: PaywallUiEvent) {
        when (event) {
            PaywallUiEvent.OnClickBuy -> buyPackage()

            PaywallUiEvent.OnClickRestore -> restorePayment()

            is PaywallUiEvent.OnSelectPackage -> {
                selectedPackageId = event.packageId
                rebuildState()
                val config = featureFlagManager.getFunnelExperimentConfig()
                calculateDownsellPrices(rawPackages, config.downsellLadder.tier2ExitIntent.discountPercent)
            }

            PaywallUiEvent.OnDismissAttempt -> handleDismissAttempt()

            PaywallUiEvent.OnClaimDownsell -> handleClaimDownsell()

            PaywallUiEvent.OnDeclineDownsell -> handleDeclineDownsell()

            PaywallUiEvent.OnBuyMicroCredit -> handleBuyMicroCredit()

            PaywallUiEvent.OnDeclineMicroCredit -> handleDeclineMicroCredit()
        }
    }

    private fun handleDismissAttempt() = viewModelScope.launch {
        if (userPreferences.hasSeenExitDownsell()) {
            _uiState.update { it.copy(isDismissRequired = true) }
            return@launch
        }

        val config = featureFlagManager.getFunnelExperimentConfig()
        if (!config.downsellLadder.tier2ExitIntent.enabled) {
            if (config.downsellLadder.tier3MicroCredit.enabled) {
                _uiState.update { it.copy(showMicroCreditDownsell = true) }
            } else {
                _uiState.update { it.copy(isDismissRequired = true) }
            }
            return@launch
        }

        val totalDurationSeconds = config.downsellLadder.tier2ExitIntent.countdownSeconds.coerceAtLeast(1)
        val now = Clock.System.now().toEpochMilliseconds()
        var startTime = userPreferences.getDownsellStartTimeMillis()
        if (startTime == null) {
            startTime = now
            userPreferences.setDownsellStartTimeMillis(now)
        }

        val elapsedSeconds = ((now - startTime) / 1000L).toInt()
        val remainingSeconds = totalDurationSeconds - elapsedSeconds

        if (remainingSeconds <= 0) {
            _uiState.update { it.copy(isDismissRequired = true) }
        } else {
            _uiState.update {
                it.copy(
                    showExitIntentDownsell = true,
                    downsellTimeRemainingSeconds = remainingSeconds,
                )
            }
            startDownsellCountdown(remainingSeconds)
        }
    }

    private fun startDownsellCountdown(initialSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var current = initialSeconds
            while (current > 0) {
                delay(1000L)
                current--
                _uiState.update { it.copy(downsellTimeRemainingSeconds = current) }
            }
            _uiState.update { it.copy(showExitIntentDownsell = false, isDismissRequired = true) }
        }
    }

    private fun handleClaimDownsell() = viewModelScope.launch {
        countdownJob?.cancel()
        userPreferences.setExitDownsellSeen(true)
        _uiState.update { it.copy(showExitIntentDownsell = false) }
        buyPackage()
    }

    private fun handleDeclineDownsell() = viewModelScope.launch {
        countdownJob?.cancel()
        userPreferences.setExitDownsellSeen(true)
        val config = featureFlagManager.getFunnelExperimentConfig()
        val showMicro = config.downsellLadder.tier3MicroCredit.enabled
        _uiState.update {
            it.copy(
                showExitIntentDownsell = false,
                showMicroCreditDownsell = showMicro,
                isDismissRequired = !showMicro,
            )
        }
    }

    private fun handleBuyMicroCredit() = viewModelScope.launch {
        _uiState.update { it.copy(showMicroCreditDownsell = false, isLoading = true) }
        val amount = _uiState.value.microCreditAmount
        creditRepository.addCredits(amount)
        AppGlobalUiState.showUiMessage(UiMessage.Resource(Res.string.paywall_msg_credits_added, amount))
        _uiState.update { it.copy(isLoading = false, isDismissRequired = true) }
    }

    private fun handleDeclineMicroCredit() {
        _uiState.update { it.copy(showMicroCreditDownsell = false, isDismissRequired = true) }
    }

    private fun calculateDownsellPrices(packages: List<PurchasePackage>, discountPercent: Int) {
        val targetPkg = packages.firstOrNull { it.id == selectedPackageId } ?: packages.firstOrNull()
        if (targetPkg != null) {
            val symbol = targetPkg.price.currencyCodeOrSymbol.orEmpty().ifBlank { "$" }
            val periodSuffix = when (targetPkg.period) {
                BillingPeriod.YEARLY -> "/năm"
                BillingPeriod.MONTHLY -> "/tháng"
                BillingPeriod.WEEKLY -> "/tuần"
                else -> ""
            }
            val original = targetPkg.price.localizedString ?: "$symbol${formatTwoDecimals(targetPkg.price.amount)}$periodSuffix"
            val discountedAmount = targetPkg.price.amount * (1f - (discountPercent.toFloat() / 100f))
            val discounted = "$symbol${formatTwoDecimals(discountedAmount)}$periodSuffix"
            _uiState.update {
                it.copy(
                    downsellOriginalPrice = original,
                    downsellDiscountedPrice = discounted,
                )
            }
        }
    }

    private fun fetchPackages() = viewModelScope.launch {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                packages = emptyList(),
                buyButtonEnabled = false,
            )
        }
        subscriptionRepository.getPackageList(placementId = placementId)
            .onSuccess { packages -> handleLoaded(packages) }
            .onFailure { error -> handleError(error) }
    }

    private fun handleLoaded(packages: List<PurchasePackage>) {
        rawPackages = if (mode == PaywallMode.CREDIT_PACK) {
            packages.sortedBy { it.price.amount }
        } else {
            packages.sortedBy { it.period?.approximateDays ?: Int.MAX_VALUE }
        }
        selectedPackageId = mapper.pickDefaultSelection(rawPackages, mode)
        rebuildState()
        val config = featureFlagManager.getFunnelExperimentConfig()
        calculateDownsellPrices(rawPackages, config.downsellLadder.tier2ExitIntent.discountPercent)
    }

    private fun handleError(error: Throwable) {
        AppLogger.e("Error getting packages: $error")
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = UiMessage.Message(error.message),
                buyButtonEnabled = false,
            )
        }
    }

    private fun rebuildState() {
        val mapped = mapper.map(rawPackages, selectedPackageId, mode)
        val hasSelection = rawPackages.any { it.id == selectedPackageId }
        _uiState.update {
            it.copy(
                isLoading = false,
                packages = mapped.packages,
                buyButtonEnabled = hasSelection,
                ctaText = mapped.ctaText,
                aboveCtaText = mapped.aboveCtaText,
                belowCtaText = mapped.belowCtaText,
            )
        }
    }

    private fun restorePayment() = viewModelScope.launch {
        if (userCanDoPaymentAction().not()) {
            AppGlobalUiState.showUiMessage(UiMessage.Resource(Res.string.paywall_msg_sign_in_required))
            _uiState.update { it.copy(signInActionRequired = true) }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        subscriptionRepository.restorePurchase()
            .onSuccess { purchaserInfo -> successfulRestore(purchaserInfo) }
            .onFailure { error -> failedRestore(error) }
    }

    private fun buyPackage() = viewModelScope.launch {
        // The mock provider simulates purchases with no backend, so it needs no signed-in user —
        // skip the sign-in gate so the demo flow works with zero config (no Firebase/auth).
        if (!subscriptionRepository.isMockProvider && userCanDoPaymentAction().not()) {
            AppGlobalUiState.showUiMessage(UiMessage.Resource(Res.string.paywall_msg_sign_in_required))
            _uiState.update { it.copy(signInActionRequired = true) }
            return@launch
        }
        val selected = rawPackages.firstOrNull { it.id == selectedPackageId } ?: return@launch
        _uiState.update { it.copy(buyButtonEnabled = false) }
        subscriptionRepository.purchase(selected.id)
            .onSuccess { purchaserInfo ->
                successfulPurchase(
                    subscriptionProviderUser = purchaserInfo,
                    productIds = listOf(selected.id.value),
                )
            }
            .onFailure { error -> failedPurchase(error) }
    }

    private fun successfulPurchase(
        subscriptionProviderUser: SubscriptionProviderUser,
        productIds: List<String>,
    ) = viewModelScope.launch {
        AppLogger.d("Successful payment, onPurchaseCompleted")
        _uiState.update { it.copy(isLoading = true) }
        val productId = productIds.firstOrNull()
        val isCreditPack = productId.isCreditPackProductId()

        if (isCreditPack && productId != null) {
            onSuccessfulCreditPack(productId)
            _uiState.update { it.copy(isDismissRequired = true, isLoading = false) }
            return@launch
        }

        val premiumSubscription = with(subscriptionRepository) {
            subscriptionProviderUser.asPremiumSubscription()
        }
        _uiState.update {
            it.copy(
                buyButtonEnabled = true,
                isLoading = false,
                successfulSubscription = premiumSubscription,
            )
        }
    }

    private fun successfulRestore(subscriptionProviderUser: SubscriptionProviderUser) = viewModelScope.launch {
        AppLogger.d("Successful restoring purchase: $subscriptionProviderUser")
        _uiState.update { it.copy(isLoading = true) }
        val premiumSubscription =
            with(subscriptionRepository) { subscriptionProviderUser.asPremiumSubscription() }
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                successfulSubscription = premiumSubscription,
            )
        }
    }

    private fun failedPurchase(error: Throwable) = viewModelScope.launch {
        AppLogger.e("There was an error with purchase: $error")
        _uiState.update {
            it.copy(
                buyButtonEnabled = true,
                errorMessage = UiMessage.Message(error.message),
            )
        }
    }

    private fun failedRestore(error: Throwable) = viewModelScope.launch {
        AppLogger.e("Error restoring purchases: $error")
        _uiState.update { state ->
            state.copy(
                errorMessage = UiMessage.Message(error.message),
                isLoading = false,
            )
        }
    }

    private suspend fun onSuccessfulCreditPack(productId: String) {
        AppLogger.d("Successful credit pack is purchased: $productId")
        val amountPart = productId.parseCreditAmountFromProductId()
        if (amountPart == null) {
            AppLogger.e(
                "Invalid credit pack product id: $productId. " +
                    "Must start with $CREDIT_PACK_PRODUCT_ID_PREFIX and contain a number. Example: credit_pack_50",
            )
            // Purchase succeeded but we can't determine the credit amount — fail loud so the
            // user (and support) know credits weren't granted, instead of silently swallowing it.
            AppGlobalUiState.showUiMessage(UiMessage.Resource(Res.string.paywall_msg_credits_not_added))
            return
        }
        AppLogger.d("Successful credit is added, amount: $amountPart")
        creditRepository.addCredits(amountPart)
        AppGlobalUiState.showUiMessage(UiMessage.Resource(Res.string.paywall_msg_credits_added, amountPart))
    }

    private suspend fun userCanDoPaymentAction(): Boolean {
        val currentUser = userRepository.currentUser.first().getOrNull()
        return currentUser != null
    }
}
