package com.kotlinfoundation.koko.growth.analytics.mmp

/**
 * Common contract for Mobile Measurement Partners (Adjust, AppsFlyer, etc.).
 */
interface MmpTracker {

    /**
     * Name of the MMP provider (e.g. "Adjust", "AppsFlyer", "None").
     */
    val providerName: String

    /**
     * Initializes the underlying MMP SDK with the provided [config].
     */
    fun initialize(config: MmpConfig)

    /**
     * Tracks an in-app event with an event token or event name.
     */
    fun trackEvent(eventTokenOrName: String, params: Map<String, Any>? = null)

    /**
     * Tracks impression ad revenue to measure campaign ROAS.
     */
    fun trackAdRevenue(
        source: String,
        value: Double,
        currency: String = "USD",
        params: Map<String, Any>? = null,
    )

    /**
     * Resolves the device attribution ID:
     * - Adjust: Adjust Device ID (`ADID`)
     * - AppsFlyer: AppsFlyer UID (`appsFlyerUID`)
     */
    suspend fun getAttributionId(timeoutMs: Long = 20_000L): String?

    /**
     * Key used when syncing this attribution ID with RevenueCat / Adapty user attributes:
     * - Adjust: `"$adjustId"`
     * - AppsFlyer: `"$appsflyerId"`
     */
    fun getRevenueCatAttributeKey(): String

    /**
     * Enables or disables attribution tracking.
     */
    fun setEnabled(enabled: Boolean)

    /**
     * Returns true if tracking is currently enabled.
     */
    fun isEnabled(): Boolean
}
