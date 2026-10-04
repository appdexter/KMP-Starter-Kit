package com.kotlinfoundation.koko.data.source.featureflag

import com.kotlinfoundation.koko.data.source.featureflag.model.CloseButtonMode
import com.kotlinfoundation.koko.data.source.featureflag.model.FunnelExperimentConfig
import com.kotlinfoundation.koko.data.source.preferences.FakeUserPreferences
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FunnelExperimentConfigTest {

    private class TestFeatureFlagManager(
        private val jsonValue: String,
    ) : FeatureFlagManager {
        override fun syncsFlagsAsync() {}
        override fun getBoolean(key: String): Boolean = false
        override fun getString(key: String): String = if (key == FeatureFlagManager.Keys.ONBOARDING_FUNNEL_CONFIG) jsonValue else ""
        override fun getLong(key: String): Long = 0L
        override fun getDouble(key: String): Double = 0.0
    }

    @Test
    fun testDefaultFallbackWhenJsonIsBlank() {
        val manager = TestFeatureFlagManager("")
        val config = manager.getFunnelExperimentConfig()

        assertEquals("deep_assessment_v3", config.funnelId)
        assertTrue(config.showOnboardingPaywall)
        assertEquals("DEEP_ASSESSMENT", config.onboardingStyle)
        assertEquals(CloseButtonMode.DELAYED, config.primaryPaywall.closeButtonMode)
        assertEquals(4000L, config.primaryPaywall.closeDelayMs)
        assertTrue(config.downsellLadder.tier2ExitIntent.enabled)
        assertEquals(40, config.downsellLadder.tier2ExitIntent.discountPercent)
        assertEquals(50, config.downsellLadder.tier3MicroCredit.creditsAmount)
    }

    @Test
    fun testParseValidCustomJsonConfig() {
        val json = """
            {
              "funnel_id": "short_carousel_test",
              "show_onboarding_paywall": false,
              "onboarding_style": "STYLE1",
              "primary_paywall": {
                "close_button_mode": "IMMEDIATE",
                "close_delay_ms": 0
              },
              "downsell_ladder": {
                "tier2_exit_intent": {
                  "enabled": false,
                  "discount_percent": 50
                },
                "tier3_micro_credit": {
                  "enabled": true,
                  "credits_amount": 100,
                  "price_text": "$9.99"
                }
              }
            }
        """.trimIndent()

        val jsonDecoder = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val decoded = jsonDecoder.decodeFromString<FunnelExperimentConfig>(json)
        assertEquals("short_carousel_test", decoded.funnelId)

        val manager = TestFeatureFlagManager(json)
        val config = manager.getFunnelExperimentConfig()

        assertEquals("short_carousel_test", config.funnelId)
        assertFalse(config.showOnboardingPaywall)
        assertEquals("STYLE1", config.onboardingStyle)
        assertEquals(CloseButtonMode.IMMEDIATE, config.primaryPaywall.closeButtonMode)
        assertEquals(0L, config.primaryPaywall.closeDelayMs)
        assertFalse(config.downsellLadder.tier2ExitIntent.enabled)
        assertEquals(50, config.downsellLadder.tier2ExitIntent.discountPercent)
        assertEquals(100, config.downsellLadder.tier3MicroCredit.creditsAmount)
        assertEquals("$9.99", config.downsellLadder.tier3MicroCredit.priceText)
    }

    @Test
    fun testUserPreferencesDownsellTracking() = runTest {
        val prefs = FakeUserPreferences()

        assertFalse(prefs.hasSeenExitDownsell())
        assertEquals(null, prefs.getDownsellStartTimeMillis())

        prefs.setExitDownsellSeen(true)
        assertTrue(prefs.hasSeenExitDownsell())

        val testTimestamp = 1727950000000L
        prefs.setDownsellStartTimeMillis(testTimestamp)
        assertEquals(testTimestamp, prefs.getDownsellStartTimeMillis())
    }
}
