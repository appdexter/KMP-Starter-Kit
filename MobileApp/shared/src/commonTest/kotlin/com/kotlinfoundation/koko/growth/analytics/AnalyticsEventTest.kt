package com.kotlinfoundation.koko.growth.analytics

import com.kotlinfoundation.koko.util.analytics.Analytics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAnalytics : Analytics {
    val loggedEvents = mutableListOf<Pair<String, Map<String, Any>?>>()
    var enabledState = true

    override fun logEvent(event: String, params: Map<String, Any>?) {
        loggedEvents.add(event to params)
    }

    override fun setEnabled(enabled: Boolean) {
        enabledState = enabled
    }
}

private class FakeAnalyticsDestination(override val name: String) : AnalyticsDestination {
    val loggedEvents = mutableListOf<Pair<String, Map<String, Any>?>>()
    var shouldThrow = false

    override fun logEvent(event: String, params: Map<String, Any>?) {
        if (shouldThrow) throw RuntimeException("Destination error")
        loggedEvents.add(event to params)
    }
}

class AnalyticsEventTest {

    @Test
    fun screenViewed_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.ScreenViewed(
            screenName = "HomeScreen",
            extraParams = mapOf("source" to "notification"),
        )
        assertEquals("screen_viewed", event.name)
        assertEquals("HomeScreen", event.params["screen_name"])
        assertEquals("notification", event.params["source"])
    }

    @Test
    fun sessionStarted_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.SessionStarted(
            sessionId = "sess_123",
            isFirstLaunch = true,
        )
        assertEquals("session_started", event.name)
        assertEquals("sess_123", event.params["session_id"])
        assertEquals(true, event.params["is_first_launch"])
    }

    @Test
    fun appOpened_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.AppOpened(
            appVersion = "1.2.3",
            platform = "Android",
        )
        assertEquals("app_opened", event.name)
        assertEquals("1.2.3", event.params["app_version"])
        assertEquals("Android", event.params["platform"])
    }

    @Test
    fun onboardingStep_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.OnboardingStep(
            stepIndex = 2,
            stepName = "GoalSelection",
            extraParams = mapOf("category" to "growth"),
        )
        assertEquals("onboarding_step", event.name)
        assertEquals(2, event.params["step_index"])
        assertEquals("GoalSelection", event.params["step_name"])
        assertEquals("growth", event.params["category"])
    }

    @Test
    fun onboardingCompleted_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.OnboardingCompleted(
            userGoal = "productivity",
            userBarrier = "procrastination",
            dailyCommitment = 15,
        )
        assertEquals("onboarding_completed", event.name)
        assertEquals("productivity", event.params["user_goal"])
        assertEquals("procrastination", event.params["user_barrier"])
        assertEquals(15, event.params["daily_commitment"])

        val eventWithNulls = AnalyticsEvent.OnboardingCompleted()
        assertEquals("onboarding_completed", eventWithNulls.name)
        assertNull(eventWithNulls.params["user_goal"])
        assertNull(eventWithNulls.params["user_barrier"])
        assertNull(eventWithNulls.params["daily_commitment"])
    }

    @Test
    fun featureUsed_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.FeatureUsed(
            featureId = "ai_avatar_gen",
            metadata = mapOf("prompt_length" to 42),
        )
        assertEquals("feature_used", event.name)
        assertEquals("ai_avatar_gen", event.params["feature_id"])
        assertEquals(42, event.params["prompt_length"])
    }

    @Test
    fun paywallViewed_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.PaywallViewed(
            placementId = "onboarding_paywall",
            triggerSource = "onboarding",
        )
        assertEquals("paywall_viewed", event.name)
        assertEquals("onboarding_paywall", event.params["placement_id"])
        assertEquals("onboarding", event.params["trigger_source"])
    }

    @Test
    fun purchaseStarted_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.PurchaseStarted(
            productId = "pro_yearly",
            price = 49.99,
            currency = "USD",
        )
        assertEquals("purchase_started", event.name)
        assertEquals("pro_yearly", event.params["product_id"])
        assertEquals(49.99, event.params["price"])
        assertEquals("USD", event.params["currency"])
    }

    @Test
    fun purchaseCompleted_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.PurchaseCompleted(
            productId = "pro_yearly",
            price = 49.99,
            currency = "USD",
            eventId = "evt_999",
        )
        assertEquals("purchase_completed", event.name)
        assertEquals("pro_yearly", event.params["product_id"])
        assertEquals(49.99, event.params["price"])
        assertEquals("USD", event.params["currency"])
        assertEquals("evt_999", event.params["event_id"])
    }

    @Test
    fun purchaseFailed_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.PurchaseFailed(
            productId = "pro_monthly",
            errorReason = "user_cancelled",
        )
        assertEquals("purchase_failed", event.name)
        assertEquals("pro_monthly", event.params["product_id"])
        assertEquals("user_cancelled", event.params["error_reason"])
    }

    @Test
    fun adImpression_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.AdImpression(
            adPlacement = "rewarded_interstitial",
            adNetwork = "admob",
            valueMicros = 15000L,
            currency = "USD",
            adFormat = "REWARDED",
        )
        assertEquals("ad_impression", event.name)
        assertEquals("rewarded_interstitial", event.params["ad_placement"])
        assertEquals("admob", event.params["ad_network"])
        assertEquals(15000L, event.params["value_micros"])
        assertEquals("USD", event.params["currency"])
        assertEquals("REWARDED", event.params["ad_format"])
    }

    @Test
    fun customEvent_hasCorrectNameAndParams() {
        val event = AnalyticsEvent.Custom(
            name = "referral_shared",
            params = mapOf("channel" to "whatsapp"),
        )
        assertEquals("referral_shared", event.name)
        assertEquals("whatsapp", event.params["channel"])
    }

    @Test
    fun analyticsTrackExtension_logsToAnalytics() {
        val fakeAnalytics = FakeAnalytics()
        val event = AnalyticsEvent.ScreenViewed("SettingsScreen")

        fakeAnalytics.track(event)

        assertEquals(1, fakeAnalytics.loggedEvents.size)
        val (eventName, eventParams) = fakeAnalytics.loggedEvents.first()
        assertEquals("screen_viewed", eventName)
        assertEquals("SettingsScreen", eventParams?.get("screen_name"))
    }

    @Test
    fun analyticsRouter_dispatchesToPrimaryAndDestinations() {
        val primary = FakeAnalytics()
        val destinationA = FakeAnalyticsDestination("Mixpanel")
        val destinationB = FakeAnalyticsDestination("AppsFlyer")

        val router = AnalyticsRouter(
            primaryAnalytics = primary,
            destinations = listOf(destinationA),
            isLoggingEnabled = true,
        )
        router.registerDestination(destinationB)

        router.track(AnalyticsEvent.FeatureUsed("export_pdf"))

        assertEquals(1, primary.loggedEvents.size)
        assertEquals("feature_used", primary.loggedEvents.first().first)

        assertEquals(1, destinationA.loggedEvents.size)
        assertEquals("feature_used", destinationA.loggedEvents.first().first)

        assertEquals(1, destinationB.loggedEvents.size)
        assertEquals("feature_used", destinationB.loggedEvents.first().first)
    }

    @Test
    fun analyticsRouter_destinationFailureDoesNotCrashOrBlockOthers() {
        val primary = FakeAnalytics()
        val faultyDestination = FakeAnalyticsDestination("Faulty").apply { shouldThrow = true }
        val goodDestination = FakeAnalyticsDestination("Good")

        val router = AnalyticsRouter(
            primaryAnalytics = primary,
            destinations = listOf(faultyDestination, goodDestination),
        )

        // Should not throw
        router.track(AnalyticsEvent.ScreenViewed("Explore"))

        assertEquals(1, primary.loggedEvents.size)
        assertEquals(1, goodDestination.loggedEvents.size)
        assertEquals(0, faultyDestination.loggedEvents.size)
    }

    @Test
    fun analyticsRouter_setEnabled_delegatesToPrimary() {
        val primary = FakeAnalytics()
        val router = AnalyticsRouter(primaryAnalytics = primary)

        router.setEnabled(false)
        assertFalse(primary.enabledState)

        router.setEnabled(true)
        assertTrue(primary.enabledState)
    }
}
