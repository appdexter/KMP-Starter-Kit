package com.kotlinfoundation.koko.growth.analytics.mmp

import com.kotlinfoundation.koko.util.analytics.Analytics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MmpAnalyticsDestinationTest {

    private class FakeMmpTracker(
        override val providerName: String = "TestMmp",
        private val revenueCatKey: String = "\$testMmpId",
    ) : MmpTracker {
        var initializedConfig: MmpConfig? = null
        val trackedEvents = mutableListOf<Pair<String, Map<String, Any>?>>()
        val trackedAdRevenues = mutableListOf<AdRevenueRecord>()
        var trackingEnabled: Boolean = true
        var fakeAttributionId: String? = "test_attribution_uid_123"

        data class AdRevenueRecord(
            val source: String,
            val value: Double,
            val currency: String,
            val params: Map<String, Any>?,
        )

        override fun initialize(config: MmpConfig) {
            initializedConfig = config
        }

        override fun trackEvent(eventTokenOrName: String, params: Map<String, Any>?) {
            trackedEvents.add(eventTokenOrName to params)
        }

        override fun trackAdRevenue(
            source: String,
            value: Double,
            currency: String,
            params: Map<String, Any>?,
        ) {
            trackedAdRevenues.add(AdRevenueRecord(source, value, currency, params))
        }

        override suspend fun getAttributionId(timeoutMs: Long): String? = fakeAttributionId

        override fun getRevenueCatAttributeKey(): String = revenueCatKey

        override fun setEnabled(enabled: Boolean) {
            trackingEnabled = enabled
        }

        override fun isEnabled(): Boolean = trackingEnabled
    }

    @Test
    fun mmpConfig_autoDetectsActiveProvider() {
        val noneConfig = MmpConfig()
        assertEquals(MmpProvider.NONE, noneConfig.activeProvider)
        assertFalse(noneConfig.hasValidConfiguration(isAndroid = true))

        val adjustConfig = MmpConfig(
            adjustAppTokenAndroid = "adjust_android_token",
            adjustAppTokenIos = "adjust_ios_token",
        )
        assertEquals(MmpProvider.ADJUST, adjustConfig.activeProvider)
        assertTrue(adjustConfig.hasValidConfiguration(isAndroid = true))
        assertTrue(adjustConfig.hasValidConfiguration(isAndroid = false))

        val appsFlyerConfig = MmpConfig(
            appsFlyerDevKey = "af_dev_key_123",
            appsFlyerAppIdIos = "123456789",
        )
        assertEquals(MmpProvider.APPSFLYER, appsFlyerConfig.activeProvider)
        assertTrue(appsFlyerConfig.hasValidConfiguration(isAndroid = true))
        assertTrue(appsFlyerConfig.hasValidConfiguration(isAndroid = false))

        // Explicit override takes precedence
        val explicitConfig = MmpConfig(
            provider = MmpProvider.APPSFLYER,
            adjustAppTokenAndroid = "adjust_token",
            appsFlyerDevKey = "af_key",
        )
        assertEquals(MmpProvider.APPSFLYER, explicitConfig.activeProvider)
    }

    @Test
    fun destination_tracksMappedEventsForAdjust() {
        val fakeTracker = FakeMmpTracker(providerName = "Adjust", revenueCatKey = "\$adjustId")
        val config = MmpConfig(
            provider = MmpProvider.ADJUST,
            adjustAppTokenAndroid = "adjust_token",
            adjustEventTokens = mapOf(
                "onboarding_completed" to "onb_tok_1",
                "custom_achievement" to "ach_tok_2",
            ),
        )
        val destination = MmpAnalyticsDestination(fakeTracker, config)

        destination.logEvent("onboarding_completed", mapOf("step" to 5))
        destination.logEvent("unmapped_event", mapOf("foo" to "bar"))
        destination.logEvent("custom_achievement", null)

        assertEquals(2, fakeTracker.trackedEvents.size)
        assertEquals("onb_tok_1", fakeTracker.trackedEvents[0].first)
        assertEquals(5, fakeTracker.trackedEvents[0].second?.get("step"))
        assertEquals("ach_tok_2", fakeTracker.trackedEvents[1].first)
        assertNull(fakeTracker.trackedEvents[1].second)
    }

    @Test
    fun destination_tracksMappedEventsForAppsFlyer() {
        val fakeTracker = FakeMmpTracker(providerName = "AppsFlyer", revenueCatKey = "\$appsflyerId")
        val config = MmpConfig(
            provider = MmpProvider.APPSFLYER,
            appsFlyerDevKey = "af_dev_key",
            appsFlyerEventTokens = mapOf(
                "onboarding_completed" to "af_complete_registration",
                "subscribe" to "af_subscribe",
            ),
        )
        val destination = MmpAnalyticsDestination(fakeTracker, config)

        destination.logEvent("onboarding_completed", mapOf("method" to "google"))
        assertEquals(1, fakeTracker.trackedEvents.size)
        assertEquals("af_complete_registration", fakeTracker.trackedEvents[0].first)
    }

    @Test
    fun destination_interceptsAdImpressionAndForwardsToAdRevenue() {
        val fakeTracker = FakeMmpTracker()
        val config = MmpConfig(isAdRevenueTrackingEnabled = true)
        val destination = MmpAnalyticsDestination(fakeTracker, config)

        val impressionParams = mapOf<String, Any>(
            Analytics.PARAM_AD_PLATFORM to "admob",
            Analytics.PARAM_VALUE to 0.025,
            Analytics.PARAM_CURRENCY to "USD",
            Analytics.PARAM_AD_FORMAT to "rewarded",
        )

        destination.logEvent(Analytics.EVENT_AD_IMPRESSION, impressionParams)

        assertEquals(1, fakeTracker.trackedAdRevenues.size)
        val record = fakeTracker.trackedAdRevenues.first()
        assertEquals("admob", record.source)
        assertEquals(0.025, record.value)
        assertEquals("USD", record.currency)
    }

    @Test
    fun destination_convertsMicrosValueToStandardUnits() {
        val fakeTracker = FakeMmpTracker()
        val config = MmpConfig(isAdRevenueTrackingEnabled = true)
        val destination = MmpAnalyticsDestination(fakeTracker, config)

        val impressionParams = mapOf<String, Any>(
            "ad_network" to "applovin_max",
            "value_micros" to 2_500_000L,
            Analytics.PARAM_CURRENCY to "USD",
        )

        destination.logEvent("ad_impression", impressionParams)

        assertEquals(1, fakeTracker.trackedAdRevenues.size)
        val record = fakeTracker.trackedAdRevenues.first()
        assertEquals("applovin_max", record.source)
        assertEquals(2.5, record.value)
    }

    @Test
    fun destination_ignoresWhenDisabled() {
        val fakeTracker = FakeMmpTracker()
        val config = MmpConfig(
            isEnabled = false,
            provider = MmpProvider.ADJUST,
            adjustEventTokens = mapOf("test_event" to "tok_1"),
        )
        val destination = MmpAnalyticsDestination(fakeTracker, config)

        destination.logEvent("test_event", emptyMap())
        assertEquals(0, fakeTracker.trackedEvents.size)

        val enabledConfig = MmpConfig(
            isEnabled = true,
            provider = MmpProvider.ADJUST,
            adjustEventTokens = mapOf("test_event" to "tok_1"),
        )
        fakeTracker.setEnabled(false)
        val destinationWithDisabledTracker = MmpAnalyticsDestination(fakeTracker, enabledConfig)
        destinationWithDisabledTracker.logEvent("test_event", emptyMap())
        assertEquals(0, fakeTracker.trackedEvents.size)
    }
}
