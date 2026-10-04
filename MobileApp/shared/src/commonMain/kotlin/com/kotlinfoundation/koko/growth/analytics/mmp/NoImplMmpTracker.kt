package com.kotlinfoundation.koko.growth.analytics.mmp

import com.kotlinfoundation.koko.util.logging.AppLogger

/**
 * Fallback / No-op implementation of [MmpTracker] for non-mobile platforms (Desktop, Web)
 * or when no MMP provider is configured.
 */
object NoImplMmpTracker : MmpTracker {

    private var enabled: Boolean = true

    override val providerName: String = "None"

    override fun initialize(config: MmpConfig) {
        safeLog("MMP initialized in NoImpl stub (no-op).")
    }

    override fun trackEvent(eventTokenOrName: String, params: Map<String, Any>?) {
        if (!enabled) return
        safeLog("MMP trackEvent [target=$eventTokenOrName, params=$params] (no-op).")
    }

    override fun trackAdRevenue(
        source: String,
        value: Double,
        currency: String,
        params: Map<String, Any>?,
    ) {
        if (!enabled) return
        safeLog("MMP trackAdRevenue [source=$source, value=$value $currency] (no-op).")
    }

    override suspend fun getAttributionId(timeoutMs: Long): String? = null

    override fun getRevenueCatAttributeKey(): String = ""

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun isEnabled(): Boolean = enabled

    private fun safeLog(message: String) {
        try {
            AppLogger.d("NoImplMmpTracker: $message")
        } catch (_: Throwable) {
            // Ignored if logger or Koin is uninitialized
        }
    }
}
