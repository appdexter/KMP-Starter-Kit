package com.kotlinfoundation.koko.growth.experiment

import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.identity.SessionManager
import kotlin.math.abs

/**
 * Deterministic A/B testing experiment allocation engine.
 * Computes a stable variant per user/experiment using hash of anonymousId + experimentId.
 * Supports manual variant overrides for development and QA testing.
 */
class ExperimentEngine(
    private val sessionManager: SessionManager,
    private val userPreferences: UserPreferences,
) {

    private val overriddenExperiments = mutableSetOf<String>()

    /**
     * Resolves the experiment variant for the current user.
     * 1. Checks preference overrides first (e.g. set by QA in debug menu).
     * 2. Otherwise deterministically hashes (anonymousId + "_" + experimentId) % 100
     *    and distributes equally across [variants].
     */
    suspend fun getVariant(
        experimentId: String,
        variants: List<String> = listOf("control", "variant_a"),
    ): String {
        if (variants.isEmpty()) return "control"

        val override = userPreferences.getString("override_exp_$experimentId", "")
        if (!override.isNullOrBlank()) {
            return override
        }

        val id = sessionManager.getAnonymousId()
        val hash = abs((id + "_" + experimentId).hashCode()) % 100
        val index = ((hash * variants.size) / 100).coerceIn(0, variants.size - 1)
        return variants[index]
    }

    /**
     * Overrides a variant for testing. Pass null or blank to clear override for this experiment.
     */
    suspend fun overrideVariant(experimentId: String, variant: String?) {
        val prefKey = "override_exp_$experimentId"
        if (variant.isNullOrBlank()) {
            userPreferences.remove(prefKey)
            overriddenExperiments.remove(experimentId)
        } else {
            userPreferences.putString(prefKey, variant)
            overriddenExperiments.add(experimentId)
        }
        syncOverriddenKeys()
    }

    /**
     * Clears all variant overrides.
     */
    suspend fun clearOverrides() {
        val persistedKeys = (userPreferences.getString(KEY_OVERRIDDEN_EXPERIMENTS, "") ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .toSet()
        val allKeys = persistedKeys + overriddenExperiments
        for (expId in allKeys) {
            userPreferences.remove("override_exp_$expId")
        }
        userPreferences.remove(KEY_OVERRIDDEN_EXPERIMENTS)
        overriddenExperiments.clear()
    }

    private suspend fun syncOverriddenKeys() {
        val currentKeys = (userPreferences.getString(KEY_OVERRIDDEN_EXPERIMENTS, "") ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()
        currentKeys.addAll(overriddenExperiments)
        if (currentKeys.isEmpty()) {
            userPreferences.remove(KEY_OVERRIDDEN_EXPERIMENTS)
        } else {
            userPreferences.putString(KEY_OVERRIDDEN_EXPERIMENTS, currentKeys.joinToString(","))
        }
    }

    companion object {
        private const val KEY_OVERRIDDEN_EXPERIMENTS = "override_experiment_keys"
    }
}
