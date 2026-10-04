package com.kotlinfoundation.koko.growth.analytics

/**
 * Destination interface for dispatching analytics events to third-party providers
 * (e.g. Firebase, Mixpanel, AppsFlyer, PostHog, or custom server endpoints).
 */
interface AnalyticsDestination {
    val name: String
    fun logEvent(event: String, params: Map<String, Any>?)
}
