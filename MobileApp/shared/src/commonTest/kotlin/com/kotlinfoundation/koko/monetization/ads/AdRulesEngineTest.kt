@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.kotlinfoundation.koko.monetization.ads

import com.kotlinfoundation.koko.data.source.featureflag.FeatureFlagManager
import com.kotlinfoundation.koko.data.source.preferences.FakeUserPreferences
import com.kotlinfoundation.koko.identity.SessionManager
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdRulesEngineTest {

    private lateinit var featureFlagManager: FakeFeatureFlagManager
    private lateinit var userPreferences: FakeUserPreferences
    private lateinit var sessionManager: SessionManager
    private lateinit var adRulesEngine: AdRulesEngine

    @BeforeTest
    fun setUp() {
        featureFlagManager = FakeFeatureFlagManager()
        userPreferences = FakeUserPreferences()
        sessionManager = SessionManager(initialAnonymousId = "test_user_ad_123")
        adRulesEngine = AdRulesEngine(sessionManager, featureFlagManager, userPreferences)

        // Enable ads by default for testing specific rule logic
        featureFlagManager.setBoolean(FeatureFlagManager.Keys.IS_ADS_ENABLED, true)
    }

    @Test
    fun canShowAd_returnsFalse_whenAdsFeatureFlagIsDisabled() {
        featureFlagManager.setBoolean(FeatureFlagManager.Keys.IS_ADS_ENABLED, false)

        val placement = AdPlacement.INTERSTITIAL_EXPORT
        adRulesEngine.setRule(placement, AdRule(minActionThreshold = 1))
        adRulesEngine.recordAction(placement)

        assertFalse(adRulesEngine.canShowAd(placement))
    }

    @Test
    fun canShowAd_returnsFalse_whenRuleIsDisabled() {
        val placement = AdPlacement.INTERSTITIAL_EXPORT
        adRulesEngine.setRule(placement, AdRule(isEnabled = false, minActionThreshold = 1))
        adRulesEngine.recordAction(placement)

        assertFalse(adRulesEngine.canShowAd(placement))
    }

    @Test
    fun canShowAd_returnsFalse_whenUserIsPremiumAndRuleExcludesPremium() {
        sessionManager.setPremium(true)

        val placement = AdPlacement.INTERSTITIAL_EXPORT
        adRulesEngine.setRule(placement, AdRule(excludePremium = true, minActionThreshold = 1))
        adRulesEngine.recordAction(placement)

        assertFalse(adRulesEngine.canShowAd(placement))
    }

    @Test
    fun canShowAd_returnsTrue_whenUserIsPremiumAndRuleDoesNotExcludePremium() {
        sessionManager.setPremium(true)

        val placement = AdPlacement.REWARDED_UNLOCK
        adRulesEngine.setRule(placement, AdRule(excludePremium = false, minActionThreshold = 1))
        adRulesEngine.recordAction(placement)

        assertTrue(adRulesEngine.canShowAd(placement))
    }

    @Test
    fun canShowAd_respectsActionThreshold() {
        val placement = AdPlacement.BANNER_MAIN
        adRulesEngine.setRule(placement, AdRule(minActionThreshold = 3))

        assertEquals(1, adRulesEngine.recordAction(placement))
        assertFalse(adRulesEngine.canShowAd(placement))

        assertEquals(2, adRulesEngine.recordAction(placement))
        assertFalse(adRulesEngine.canShowAd(placement))

        assertEquals(3, adRulesEngine.recordAction(placement))
        assertTrue(adRulesEngine.canShowAd(placement))
    }

    @Test
    fun canShowAd_respectsCooldownPeriod() {
        val placement = AdPlacement.INTERSTITIAL_EXPORT
        val rule = AdRule(minActionThreshold = 1, cooldownSeconds = 60L)
        adRulesEngine.setRule(placement, rule)

        val t0 = 100_000L
        adRulesEngine.recordAction(placement)
        assertTrue(adRulesEngine.canShowAd(placement, currentTimeMillis = t0))

        adRulesEngine.recordAdShown(placement, timestampMillis = t0)

        // Reset and record action again
        adRulesEngine.recordAction(placement)

        // During cooldown (30 seconds later)
        val t30 = t0 + 30_000L
        assertFalse(adRulesEngine.canShowAd(placement, currentTimeMillis = t30))

        // After cooldown passes (61 seconds later)
        val t61 = t0 + 61_000L
        assertTrue(adRulesEngine.canShowAd(placement, currentTimeMillis = t61))
    }

    @Test
    fun recordAdShown_resetsActionCount() {
        val placement = AdPlacement.INTERSTITIAL_EXPORT
        adRulesEngine.setRule(placement, AdRule(minActionThreshold = 2, cooldownSeconds = 0L))

        adRulesEngine.recordAction(placement)
        adRulesEngine.recordAction(placement)
        assertTrue(adRulesEngine.canShowAd(placement, currentTimeMillis = 10_000L))

        adRulesEngine.recordAdShown(placement, timestampMillis = 10_000L)

        // Actions should be 0 now, so threshold not met
        assertFalse(adRulesEngine.canShowAd(placement, currentTimeMillis = 10_001L))
    }

    @Test
    fun defaultRules_areInitializedCorrectlyForAllPlacements() {
        val bannerRule = adRulesEngine.getRule(AdPlacement.BANNER_MAIN)
        assertTrue(bannerRule.isEnabled)
        assertEquals(0, bannerRule.minActionThreshold)
        assertEquals(0L, bannerRule.cooldownSeconds)
        assertTrue(bannerRule.excludePremium)

        val interstitialRule = adRulesEngine.getRule(AdPlacement.INTERSTITIAL_EXPORT)
        assertTrue(interstitialRule.isEnabled)
        assertEquals(2, interstitialRule.minActionThreshold)
        assertEquals(45L, interstitialRule.cooldownSeconds)
        assertTrue(interstitialRule.excludePremium)

        val rewardedRule = adRulesEngine.getRule(AdPlacement.REWARDED_UNLOCK)
        assertTrue(rewardedRule.isEnabled)
        assertEquals(0, rewardedRule.minActionThreshold)
        assertEquals(0L, rewardedRule.cooldownSeconds)
        assertFalse(rewardedRule.excludePremium)

        val appOpenRule = adRulesEngine.getRule(AdPlacement.APP_OPEN)
        assertTrue(appOpenRule.isEnabled)
        assertEquals(0, appOpenRule.minActionThreshold)
        assertEquals(120L, appOpenRule.cooldownSeconds)
        assertTrue(appOpenRule.excludePremium)
    }

    @Test
    fun triggerAdIfAllowed_executesCallbackAndReturnsTrue_whenAllowed() {
        val placement = AdPlacement.BANNER_MAIN
        var callbackExecuted = false

        val result = adRulesEngine.triggerAdIfAllowed(placement) {
            callbackExecuted = true
        }

        assertTrue(result)
        assertTrue(callbackExecuted)
    }

    @Test
    fun triggerAdIfAllowed_skipsCallbackAndReturnsFalse_whenDisallowed() {
        featureFlagManager.setBoolean(FeatureFlagManager.Keys.IS_ADS_ENABLED, false)
        val placement = AdPlacement.BANNER_MAIN
        var callbackExecuted = false

        val result = adRulesEngine.triggerAdIfAllowed(placement) {
            callbackExecuted = true
        }

        assertFalse(result)
        assertFalse(callbackExecuted)
    }

    @Test
    fun recordAdShown_persistsTimestampToUserPreferences_whenScopeProvided() = runTest {
        val testEngine = AdRulesEngine(
            sessionManager = sessionManager,
            featureFlagManager = featureFlagManager,
            userPreferences = userPreferences,
            scope = this,
        )
        val placement = AdPlacement.BANNER_MAIN
        testEngine.recordAdShown(placement, timestampMillis = 88888L)
        runCurrent()

        val stored = userPreferences.getLong("ad_last_shown_${placement.placementKey}")
        assertEquals(88888L, stored)
    }

    private class FakeFeatureFlagManager : FeatureFlagManager {
        private val flags = mutableMapOf<String, Any>()

        fun setBoolean(key: String, value: Boolean) {
            flags[key] = value
        }

        override fun getBoolean(key: String): Boolean = flags[key] as? Boolean ?: false
        override fun getString(key: String): String = flags[key] as? String ?: ""
        override fun getLong(key: String): Long = flags[key] as? Long ?: 0L
        override fun getDouble(key: String): Double = flags[key] as? Double ?: 0.0
        override fun syncsFlagsAsync() {}
    }
}
