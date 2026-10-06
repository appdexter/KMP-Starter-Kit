package com.kotlinfoundation.koko.monetization.ads

import com.kotlinfoundation.koko.data.source.featureflag.FeatureFlagManager
import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.identity.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Manages ad display conditions, frequency capping, cooldown periods, and action thresholds.
 */
class AdRulesEngine(
    private val sessionManager: SessionManager,
    private val featureFlagManager: FeatureFlagManager,
    private val userPreferences: UserPreferences,
    private val scope: CoroutineScope? = null,
) {
    private val rules = mutableMapOf<AdPlacement, AdRule>()
    private val actionCounts = mutableMapOf<AdPlacement, Int>()
    private val lastAdTimes = mutableMapOf<AdPlacement, Long>()

    init {
        rules[AdPlacement.BANNER_MAIN] = AdRule(isEnabled = true, minActionThreshold = 0, cooldownSeconds = 0L, excludePremium = true)
        rules[AdPlacement.INTERSTITIAL_EXPORT] = AdRule(isEnabled = true, minActionThreshold = 2, cooldownSeconds = 45L, excludePremium = true)
        rules[AdPlacement.REWARDED_UNLOCK] = AdRule(isEnabled = true, minActionThreshold = 0, cooldownSeconds = 0L, excludePremium = false)
        rules[AdPlacement.APP_OPEN] = AdRule(isEnabled = true, minActionThreshold = 0, cooldownSeconds = 120L, excludePremium = true)
    }

    fun setRule(placement: AdPlacement, rule: AdRule) {
        rules[placement] = rule
    }

    fun getRule(placement: AdPlacement): AdRule = rules[placement] ?: AdRule()

    fun recordAction(placement: AdPlacement): Int {
        val newCount = (actionCounts[placement] ?: 0) + 1
        actionCounts[placement] = newCount
        return newCount
    }

    fun recordAdShown(
        placement: AdPlacement,
        timestampMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ) {
        lastAdTimes[placement] = timestampMillis
        actionCounts[placement] = 0
        scope?.launch {
            userPreferences.putLong("ad_last_shown_${placement.placementKey}", timestampMillis)
        }
    }

    fun triggerAdIfAllowed(
        placement: AdPlacement,
        currentTimeMillis: Long = Clock.System.now().toEpochMilliseconds(),
        onAllowed: () -> Unit,
    ): Boolean {
        if (canShowAd(placement, currentTimeMillis)) {
            recordAdShown(placement, currentTimeMillis)
            onAllowed()
            return true
        }
        return false
    }

    fun canShowAd(
        placement: AdPlacement,
        currentTimeMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): Boolean {
        if (!featureFlagManager.getBoolean(FeatureFlagManager.Keys.IS_ADS_ENABLED)) {
            return false
        }

        val rule = getRule(placement)
        if (!rule.isEnabled) {
            return false
        }

        if (rule.excludePremium && sessionManager.userIdentity.value.isPremium) {
            return false
        }

        val lastAdTime = lastAdTimes[placement]
        if (lastAdTime != null && lastAdTime > 0L) {
            val elapsed = currentTimeMillis - lastAdTime
            if (elapsed < rule.cooldownSeconds * 1000L) {
                return false
            }
        }

        val count = actionCounts[placement] ?: 0
        if (count < rule.minActionThreshold) {
            return false
        }

        return true
    }
}
