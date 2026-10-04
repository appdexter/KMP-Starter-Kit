package com.kotlinfoundation.koko.growth.analytics.mmp

import android.content.Context

/**
 * Composite [MmpTracker] for Android that routes calls to either [AndroidAdjustTracker]
 * or [AndroidAppsFlyerTracker] based on [MmpConfig.activeProvider].
 */
class CompositeAndroidMmpTracker(
    context: Context,
) : MmpTracker {

    private val adjustTracker = AndroidAdjustTracker(context)
    private val appsFlyerTracker = AndroidAppsFlyerTracker(context)
    private var activeTracker: MmpTracker = NoImplMmpTracker

    override val providerName: String
        get() = activeTracker.providerName

    override fun initialize(config: MmpConfig) {
        activeTracker = when (config.activeProvider) {
            MmpProvider.ADJUST -> adjustTracker
            MmpProvider.APPSFLYER -> appsFlyerTracker
            MmpProvider.NONE -> NoImplMmpTracker
        }
        activeTracker.initialize(config)
    }

    override fun trackEvent(eventTokenOrName: String, params: Map<String, Any>?) {
        activeTracker.trackEvent(eventTokenOrName, params)
    }

    override fun trackAdRevenue(
        source: String,
        value: Double,
        currency: String,
        params: Map<String, Any>?,
    ) {
        activeTracker.trackAdRevenue(source, value, currency, params)
    }

    override suspend fun getAttributionId(timeoutMs: Long): String? = activeTracker.getAttributionId(timeoutMs)

    override fun getRevenueCatAttributeKey(): String = activeTracker.getRevenueCatAttributeKey()

    override fun setEnabled(enabled: Boolean) {
        activeTracker.setEnabled(enabled)
    }

    override fun isEnabled(): Boolean = activeTracker.isEnabled()
}
