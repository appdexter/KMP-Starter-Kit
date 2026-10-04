package com.kotlinfoundation.koko.growth.analytics.mmp

import com.kotlinfoundation.koko.growth.analytics.AnalyticsDestination
import com.kotlinfoundation.koko.util.analytics.Analytics

/**
 * Universal [AnalyticsDestination] bridge connecting [AnalyticsRouter] to the active [MmpTracker].
 * Supports both Adjust and AppsFlyer seamlessly.
 */
class MmpAnalyticsDestination(
    private val mmpTracker: MmpTracker,
    private val config: MmpConfig,
) : AnalyticsDestination {

    override val name: String
        get() = "MMP-${mmpTracker.providerName}"

    override fun logEvent(event: String, params: Map<String, Any>?) {
        if (!config.isEnabled || !mmpTracker.isEnabled()) return

        // 1. Intercept Ad Revenue impressions (AdMob -> MMP ROAS)
        if (config.isAdRevenueTrackingEnabled && isAdImpressionEvent(event)) {
            forwardAdRevenue(params)
        }

        // 2. Map standard event to MMP token or custom event name
        val mappedToken = config.eventTokens[event]
        if (!mappedToken.isNullOrBlank()) {
            mmpTracker.trackEvent(mappedToken, params)
        }
    }

    private fun isAdImpressionEvent(event: String): Boolean = event == Analytics.EVENT_AD_IMPRESSION || event == "ad_impression"

    private fun forwardAdRevenue(params: Map<String, Any>?) {
        if (params == null) return

        val adPlatform = (params[Analytics.PARAM_AD_PLATFORM] as? String)
            ?: (params["ad_network"] as? String)
            ?: "admob_sdk"

        val currency = (params[Analytics.PARAM_CURRENCY] as? String) ?: "USD"

        val revenueValue: Double = when {
            params.containsKey(Analytics.PARAM_VALUE) -> {
                when (val v = params[Analytics.PARAM_VALUE]) {
                    is Number -> v.toDouble()
                    is String -> v.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
            }

            params.containsKey("value_micros") -> {
                when (val v = params["value_micros"]) {
                    is Number -> v.toDouble() / 1_000_000.0
                    is String -> (v.toDoubleOrNull() ?: 0.0) / 1_000_000.0
                    else -> 0.0
                }
            }

            else -> 0.0
        }

        if (revenueValue > 0.0) {
            mmpTracker.trackAdRevenue(
                source = adPlatform,
                value = revenueValue,
                currency = currency,
                params = params,
            )
        }
    }
}
