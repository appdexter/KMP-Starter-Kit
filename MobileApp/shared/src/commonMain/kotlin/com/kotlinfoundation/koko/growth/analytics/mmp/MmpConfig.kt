package com.kotlinfoundation.koko.growth.analytics.mmp

/**
 * Operating environment for MMP SDKs.
 */
enum class MmpEnvironment {
    SANDBOX,
    PRODUCTION,
}

/**
 * Unified configuration for Mobile Measurement Partner (Adjust or AppsFlyer).
 *
 * @property provider Explicit provider selection ([MmpProvider.ADJUST], [MmpProvider.APPSFLYER], or [MmpProvider.NONE]).
 * @property environment Target environment ([MmpEnvironment.SANDBOX] or [MmpEnvironment.PRODUCTION]).
 * @property attConsentWaitingIntervalSeconds Waiting interval in seconds for the user's ATT decision on iOS.
 * @property isAdRevenueTrackingEnabled Whether to forward AdMob ad impressions to the MMP for ROAS optimization.
 * @property isEnabled Master toggle for attribution tracking.
 *
 * Adjust Specific:
 * @property adjustAppTokenAndroid Adjust App Token for Android.
 * @property adjustAppTokenIos Adjust App Token for iOS.
 * @property adjustEventTokens Map of canonical event names to Adjust event tokens.
 *
 * AppsFlyer Specific:
 * @property appsFlyerDevKey AppsFlyer devKey (common for both platforms).
 * @property appsFlyerAppIdIos Apple App ID (numeric or id prefix) required by AppsFlyer on iOS.
 * @property appsFlyerEventTokens Map of canonical event names to AppsFlyer custom event names.
 */
data class MmpConfig(
    val provider: MmpProvider = MmpProvider.NONE,
    val environment: MmpEnvironment = MmpEnvironment.SANDBOX,
    val attConsentWaitingIntervalSeconds: Int = 30,
    val isAdRevenueTrackingEnabled: Boolean = true,
    val isEnabled: Boolean = true,

    // Adjust specific
    val adjustAppTokenAndroid: String = "",
    val adjustAppTokenIos: String = "",
    val adjustEventTokens: Map<String, String> = emptyMap(),

    // AppsFlyer specific
    val appsFlyerDevKey: String = "",
    val appsFlyerAppIdIos: String = "",
    val appsFlyerEventTokens: Map<String, String> = emptyMap(),
) {
    /**
     * Resolves the active provider, auto-detecting if [provider] is set to [MmpProvider.NONE].
     */
    val activeProvider: MmpProvider
        get() {
            if (provider != MmpProvider.NONE) return provider
            return when {
                adjustAppTokenAndroid.isNotBlank() || adjustAppTokenIos.isNotBlank() -> MmpProvider.ADJUST
                appsFlyerDevKey.isNotBlank() -> MmpProvider.APPSFLYER
                else -> MmpProvider.NONE
            }
        }

    /**
     * Checks if a valid token/key exists for the active platform.
     */
    fun hasValidConfiguration(isAndroid: Boolean): Boolean {
        if (!isEnabled) return false
        return when (activeProvider) {
            MmpProvider.ADJUST -> {
                val token = if (isAndroid) adjustAppTokenAndroid else adjustAppTokenIos
                token.isNotBlank()
            }

            MmpProvider.APPSFLYER -> {
                appsFlyerDevKey.isNotBlank() && (isAndroid || appsFlyerAppIdIos.isNotBlank())
            }

            MmpProvider.NONE -> false
        }
    }

    /**
     * Returns event tokens map according to the active provider.
     */
    val eventTokens: Map<String, String>
        get() = when (activeProvider) {
            MmpProvider.ADJUST -> adjustEventTokens
            MmpProvider.APPSFLYER -> appsFlyerEventTokens
            MmpProvider.NONE -> emptyMap()
        }
}
