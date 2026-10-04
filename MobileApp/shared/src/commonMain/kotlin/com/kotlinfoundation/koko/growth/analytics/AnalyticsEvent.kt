package com.kotlinfoundation.koko.growth.analytics

import com.kotlinfoundation.koko.util.analytics.Analytics

/**
 * Standardized typed analytics events following the noun_action naming convention.
 * Ensures consistent event names and parameter keys across all supported analytics destinations.
 */
sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, Any>

    data class ScreenViewed(
        val screenName: String,
        val extraParams: Map<String, Any> = emptyMap(),
    ) : AnalyticsEvent {
        override val name: String = "screen_viewed"
        override val params: Map<String, Any> = mapOf("screen_name" to screenName) + extraParams
    }

    data class SessionStarted(
        val sessionId: String,
        val isFirstLaunch: Boolean = false,
    ) : AnalyticsEvent {
        override val name: String = "session_started"
        override val params: Map<String, Any> = mapOf(
            "session_id" to sessionId,
            "is_first_launch" to isFirstLaunch,
        )
    }

    data class AppOpened(
        val appVersion: String = "",
        val platform: String = "",
    ) : AnalyticsEvent {
        override val name: String = "app_opened"
        override val params: Map<String, Any> = buildMap {
            if (appVersion.isNotEmpty()) put("app_version", appVersion)
            if (platform.isNotEmpty()) put("platform", platform)
        }
    }

    data class OnboardingStep(
        val stepIndex: Int,
        val stepName: String,
        val extraParams: Map<String, Any> = emptyMap(),
    ) : AnalyticsEvent {
        override val name: String = "onboarding_step"
        override val params: Map<String, Any> = mapOf(
            "step_index" to stepIndex,
            "step_name" to stepName,
        ) + extraParams
    }

    data class OnboardingCompleted(
        val userGoal: String? = null,
        val userBarrier: String? = null,
        val dailyCommitment: Int? = null,
    ) : AnalyticsEvent {
        override val name: String = "onboarding_completed"
        override val params: Map<String, Any> = buildMap {
            userGoal?.let { put("user_goal", it) }
            userBarrier?.let { put("user_barrier", it) }
            dailyCommitment?.let { put("daily_commitment", it) }
        }
    }

    data class FeatureUsed(
        val featureId: String,
        val metadata: Map<String, Any> = emptyMap(),
    ) : AnalyticsEvent {
        override val name: String = "feature_used"
        override val params: Map<String, Any> = mapOf("feature_id" to featureId) + metadata
    }

    data class PaywallViewed(
        val placementId: String,
        val triggerSource: String = "ui",
    ) : AnalyticsEvent {
        override val name: String = "paywall_viewed"
        override val params: Map<String, Any> = mapOf(
            "placement_id" to placementId,
            "trigger_source" to triggerSource,
        )
    }

    data class PurchaseStarted(
        val productId: String,
        val price: Double? = null,
        val currency: String = "USD",
    ) : AnalyticsEvent {
        override val name: String = "purchase_started"
        override val params: Map<String, Any> = buildMap {
            put("product_id", productId)
            price?.let { put("price", it) }
            put("currency", currency)
        }
    }

    data class PurchaseCompleted(
        val productId: String,
        val price: Double,
        val currency: String = "USD",
        val eventId: String? = null,
    ) : AnalyticsEvent {
        override val name: String = "purchase_completed"
        override val params: Map<String, Any> = buildMap {
            put("product_id", productId)
            put("price", price)
            put("currency", currency)
            eventId?.let { put("event_id", it) }
        }
    }

    data class PurchaseFailed(
        val productId: String,
        val errorReason: String,
    ) : AnalyticsEvent {
        override val name: String = "purchase_failed"
        override val params: Map<String, Any> = mapOf(
            "product_id" to productId,
            "error_reason" to errorReason,
        )
    }

    data class AdImpression(
        val adPlacement: String,
        val adNetwork: String = "admob",
        val valueMicros: Long = 0,
        val currency: String = "USD",
        val adFormat: String? = null,
    ) : AnalyticsEvent {
        override val name: String = "ad_impression"
        override val params: Map<String, Any> = buildMap {
            put("ad_placement", adPlacement)
            put("ad_network", adNetwork)
            put("value_micros", valueMicros)
            put("currency", currency)
            adFormat?.let { put("ad_format", it) }
        }
    }

    data class Custom(
        override val name: String,
        override val params: Map<String, Any> = emptyMap(),
    ) : AnalyticsEvent
}

/**
 * Convenience extension to log typed events using [Analytics].
 */
fun Analytics.track(event: AnalyticsEvent) = logEvent(event.name, event.params)
