package com.kotlinfoundation.koko.presentation.components.ads

import com.kotlinfoundation.koko.data.source.featureflag.FeatureFlagManager
import com.kotlinfoundation.koko.data.source.preferences.FakeUserPreferences
import com.kotlinfoundation.koko.identity.SessionManager
import com.kotlinfoundation.koko.monetization.ads.AdPlacement
import com.kotlinfoundation.koko.monetization.ads.AdRule
import com.kotlinfoundation.koko.monetization.ads.AdRulesEngine
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuleControlledAdDisplayerTest {

    private lateinit var featureFlagManager: FakeFeatureFlagManager
    private lateinit var userPreferences: FakeUserPreferences
    private lateinit var sessionManager: SessionManager
    private lateinit var adRulesEngine: AdRulesEngine
    private lateinit var innerDisplayer: FakeFullScreenAdDisplayer

    @BeforeTest
    fun setUp() {
        featureFlagManager = FakeFeatureFlagManager()
        userPreferences = FakeUserPreferences()
        sessionManager = SessionManager(initialAnonymousId = "test_user_rule_displayer")
        adRulesEngine = AdRulesEngine(sessionManager, featureFlagManager, userPreferences)
        innerDisplayer = FakeFullScreenAdDisplayer()

        featureFlagManager.setBoolean(FeatureFlagManager.Keys.IS_ADS_ENABLED, true)
    }

    @Test
    fun show_delegatesToInnerDisplayerAndRecordsAdShown_whenRulePasses() {
        val placement = AdPlacement.INTERSTITIAL_EXPORT
        // 0 threshold, 60s cooldown
        adRulesEngine.setRule(placement, AdRule(isEnabled = true, minActionThreshold = 0, cooldownSeconds = 60L))

        var onAdShownCallbackCalled = false
        val displayer = RuleControlledAdDisplayer(
            delegate = innerDisplayer,
            placement = placement,
            adRulesEngine = adRulesEngine,
            onAdShown = { onAdShownCallbackCalled = true },
        )

        displayer.show()

        assertEquals(1, innerDisplayer.showCount)
        assertTrue(onAdShownCallbackCalled)
        // Ad shown was recorded, so immediate next canShowAd check must be false due to cooldown
        assertFalse(adRulesEngine.canShowAd(placement))
    }

    @Test
    fun show_blocksDelegateCall_whenFeatureFlagIsDisabled() {
        featureFlagManager.setBoolean(FeatureFlagManager.Keys.IS_ADS_ENABLED, false)

        val placement = AdPlacement.BANNER_MAIN
        adRulesEngine.setRule(placement, AdRule(isEnabled = true, minActionThreshold = 0))

        var onAdShownCallbackCalled = false
        val displayer = RuleControlledAdDisplayer(
            delegate = innerDisplayer,
            placement = placement,
            adRulesEngine = adRulesEngine,
            onAdShown = { onAdShownCallbackCalled = true },
        )

        displayer.show()

        assertEquals(0, innerDisplayer.showCount)
        assertFalse(onAdShownCallbackCalled)
    }

    @Test
    fun show_blocksDelegateCall_whenCooldownHasNotPassed() {
        val placement = AdPlacement.INTERSTITIAL_EXPORT
        adRulesEngine.setRule(placement, AdRule(isEnabled = true, minActionThreshold = 0, cooldownSeconds = 60L))

        val displayer = RuleControlledAdDisplayer(
            delegate = innerDisplayer,
            placement = placement,
            adRulesEngine = adRulesEngine,
        )

        displayer.show()
        assertEquals(1, innerDisplayer.showCount)

        // Second show attempt right away during cooldown
        displayer.show()
        assertEquals(1, innerDisplayer.showCount)
    }

    @Test
    fun show_blocksDelegateCall_whenActionThresholdNotMet() {
        val placement = AdPlacement.INTERSTITIAL_EXPORT
        adRulesEngine.setRule(placement, AdRule(isEnabled = true, minActionThreshold = 3, cooldownSeconds = 0L))

        val displayer = RuleControlledAdDisplayer(
            delegate = innerDisplayer,
            placement = placement,
            adRulesEngine = adRulesEngine,
        )

        displayer.show()
        assertEquals(0, innerDisplayer.showCount)

        adRulesEngine.recordAction(placement)
        adRulesEngine.recordAction(placement)
        displayer.show()
        assertEquals(0, innerDisplayer.showCount)

        adRulesEngine.recordAction(placement) // Count is 3 now
        displayer.show()
        assertEquals(1, innerDisplayer.showCount)
    }

    @Test
    fun show_blocksDelegateCall_whenUserIsPremiumAndRuleExcludesPremium() {
        sessionManager.setPremium(true)
        val placement = AdPlacement.INTERSTITIAL_EXPORT
        adRulesEngine.setRule(placement, AdRule(isEnabled = true, minActionThreshold = 0, excludePremium = true))

        val displayer = RuleControlledAdDisplayer(
            delegate = innerDisplayer,
            placement = placement,
            adRulesEngine = adRulesEngine,
        )

        displayer.show()
        assertEquals(0, innerDisplayer.showCount)
    }

    private class FakeFullScreenAdDisplayer : FullScreenAdDisplayer {
        var showCount = 0

        override fun show() {
            showCount++
        }
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
