package com.kotlinfoundation.koko.util.analytics

// Analytics abstraction (Firebase Analytics on mobile, no-op elsewhere). Event/param name
// constants live in the companion; use logScreenView for the common screen-view event.
interface Analytics {
    fun logEvent(event: String, params: Map<String, Any>? = emptyMap())
    fun setEnabled(enabled: Boolean = true)

    companion object {
        const val EVENT_SCREEN_VIEW = "screen_view"
        const val PARAM_SCREEN_NAME = "screen_name"
        const val EVENT_PAYWALL_DISMISSED = "paywall_dismissed"
        const val PARAM_NB_PAYWALL_DISMISSED = "nb_paywall_dismissed"
        const val EVENT_CLICKED_GENERATE = "clicked_generate"
        const val EVENT_CLICKED_REPORT_AI_CONTENT = "clicked_report_ai_content"

        // ROAS tracking events
        const val EVENT_PURCHASE = "purchase"
        const val EVENT_SUBSCRIBE = "subscribe"
        const val EVENT_AD_IMPRESSION = "ad_impression"
        const val EVENT_ONBOARDING_STEP = "onboarding_step"
        const val EVENT_ONBOARDING_COMPLETED = "onboarding_completed"

        // ROAS and Onboarding param constants
        const val PARAM_VALUE = "value"
        const val PARAM_CURRENCY = "currency"
        const val PARAM_EVENT_ID = "event_id"
        const val PARAM_AD_PLATFORM = "ad_platform"
        const val PARAM_AD_SOURCE = "ad_source"
        const val PARAM_AD_FORMAT = "ad_format"
        const val PARAM_STEP_NAME = "step_name"
        const val PARAM_USER_GOAL = "user_goal"
        const val PARAM_BARRIER = "barrier"
        const val PARAM_DAILY_COMMITMENT = "daily_commitment"
    }
}

fun Analytics.logScreenView(screenName: String, params: Map<String, Any>? = emptyMap()) {
    logEvent(
        event = Analytics.EVENT_SCREEN_VIEW,
        params = mapOf(Analytics.PARAM_SCREEN_NAME to screenName) + (params ?: emptyMap()),
    )
}

fun Analytics.logPurchase(
    value: Double,
    currency: String = "USD",
    eventId: String? = null,
    params: Map<String, Any>? = emptyMap(),
) {
    val eventParams = mutableMapOf<String, Any>(
        Analytics.PARAM_VALUE to value,
        Analytics.PARAM_CURRENCY to currency,
    )
    if (eventId != null) eventParams[Analytics.PARAM_EVENT_ID] = eventId
    if (params != null) eventParams.putAll(params)
    logEvent(Analytics.EVENT_PURCHASE, eventParams)
}

fun Analytics.logSubscribe(
    value: Double? = null,
    currency: String = "USD",
    eventId: String? = null,
    params: Map<String, Any>? = emptyMap(),
) {
    val eventParams = mutableMapOf<String, Any>(
        Analytics.PARAM_CURRENCY to currency,
    )
    if (value != null) eventParams[Analytics.PARAM_VALUE] = value
    if (eventId != null) eventParams[Analytics.PARAM_EVENT_ID] = eventId
    if (params != null) eventParams.putAll(params)
    logEvent(Analytics.EVENT_SUBSCRIBE, eventParams)
}

fun Analytics.logAdImpression(
    value: Double? = null,
    currency: String = "USD",
    adPlatform: String = "admob",
    adSource: String? = null,
    adFormat: String? = null,
    params: Map<String, Any>? = emptyMap(),
) {
    val eventParams = mutableMapOf<String, Any>(
        Analytics.PARAM_AD_PLATFORM to adPlatform,
        Analytics.PARAM_CURRENCY to currency,
    )
    if (value != null) eventParams[Analytics.PARAM_VALUE] = value
    if (adSource != null) eventParams[Analytics.PARAM_AD_SOURCE] = adSource
    if (adFormat != null) eventParams[Analytics.PARAM_AD_FORMAT] = adFormat
    if (params != null) eventParams.putAll(params)
    logEvent(Analytics.EVENT_AD_IMPRESSION, eventParams)
}

fun Analytics.logOnboardingStep(
    step: Int,
    stepName: String,
    params: Map<String, Any>? = emptyMap(),
) {
    val eventParams = mutableMapOf<String, Any>(
        Analytics.PARAM_STEP_NAME to stepName,
        "step_index" to step,
    )
    if (params != null) eventParams.putAll(params)
    logEvent(Analytics.EVENT_ONBOARDING_STEP, eventParams)
}

fun Analytics.logOnboardingCompleted(
    goal: String? = null,
    barrier: String? = null,
    dailyCommitment: Int? = null,
    params: Map<String, Any>? = emptyMap(),
) {
    val eventParams = mutableMapOf<String, Any>()
    if (goal != null) eventParams[Analytics.PARAM_USER_GOAL] = goal
    if (barrier != null) eventParams[Analytics.PARAM_BARRIER] = barrier
    if (dailyCommitment != null) eventParams[Analytics.PARAM_DAILY_COMMITMENT] = dailyCommitment
    if (params != null) eventParams.putAll(params)
    logEvent(Analytics.EVENT_ONBOARDING_COMPLETED, eventParams)
}
