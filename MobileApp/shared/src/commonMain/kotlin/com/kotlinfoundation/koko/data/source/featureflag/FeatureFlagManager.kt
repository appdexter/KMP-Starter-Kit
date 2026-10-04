package com.kotlinfoundation.koko.data.source.featureflag

import com.kotlinfoundation.koko.data.source.featureflag.model.FunnelExperimentConfig
import kotlinx.serialization.json.Json

/**
 * Remote feature flags (backed by Firebase Remote Config on Android/iOS). Flags fall back to
 * [DEFAULT_VALUES] until [syncsFlagsAsync] fetches fresh values, and on platforms without an
 * implementation (see [NoImplFeatureFlagManager]).
 */
interface FeatureFlagManager {

    object Keys {
        const val IS_ADS_ENABLED = "is_ads_enabled"
        const val IS_ANALYTICS_ENABLED = "is_analytics_enabled"
        const val SHOW_REMOTE_PAYWALL = "show_remote_paywall"
        const val ONBOARDING_FUNNEL_CONFIG = "onboarding_funnel_config"
    }

    companion object {
        // Add Optional Default Feature Flag Values Here
        val DEFAULT_VALUES: Map<String, Any> = mapOf(
            Keys.IS_ADS_ENABLED to false,
            Keys.IS_ANALYTICS_ENABLED to true,
            Keys.SHOW_REMOTE_PAYWALL to false, // Set to true to use the provider's built-in remote paywall (Adapty/RevenueCat UI) instead of the custom one.
            Keys.ONBOARDING_FUNNEL_CONFIG to "{}",
        )
    }

    fun syncsFlagsAsync()
    fun getBoolean(key: String): Boolean
    fun getString(key: String): String
    fun getLong(key: String): Long
    fun getDouble(key: String): Double
}

private val funnelExperimentJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

fun FeatureFlagManager.getFunnelExperimentConfig(): FunnelExperimentConfig {
    val rawJson = getString(FeatureFlagManager.Keys.ONBOARDING_FUNNEL_CONFIG)
    if (rawJson.isBlank()) {
        return FunnelExperimentConfig()
    }
    return try {
        funnelExperimentJson.decodeFromString<FunnelExperimentConfig>(rawJson)
    } catch (_: Throwable) {
        FunnelExperimentConfig()
    }
}
