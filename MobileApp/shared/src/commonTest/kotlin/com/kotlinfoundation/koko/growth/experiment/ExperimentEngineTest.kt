package com.kotlinfoundation.koko.growth.experiment

import com.kotlinfoundation.koko.data.source.preferences.FakeUserPreferences
import com.kotlinfoundation.koko.identity.SessionManager
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ExperimentEngineTest {

    private lateinit var userPreferences: FakeUserPreferences
    private lateinit var sessionManager: SessionManager
    private lateinit var experimentEngine: ExperimentEngine

    @BeforeTest
    fun setUp() {
        userPreferences = FakeUserPreferences()
        sessionManager = SessionManager(initialAnonymousId = "test_user_deterministic_123")
        experimentEngine = ExperimentEngine(sessionManager, userPreferences)
    }

    @Test
    fun getVariant_isDeterministicForSameUserAndExperiment() = runTest {
        val expId = "onboarding_layout_v2"
        val variants = listOf("control", "variant_a", "variant_b")

        val firstCall = experimentEngine.getVariant(expId, variants)
        val secondCall = experimentEngine.getVariant(expId, variants)
        val thirdCall = experimentEngine.getVariant(expId, variants)

        assertEquals(firstCall, secondCall)
        assertEquals(secondCall, thirdCall)
        assertTrue(variants.contains(firstCall))
    }

    @Test
    fun getVariant_differentUsersCanReceiveDifferentVariants() = runTest {
        val expId = "paywall_pricing_test"
        val variants = listOf("monthly_first", "yearly_first")

        val userResults = mutableSetOf<String>()
        // Test with different anonymous users
        for (i in 1..20) {
            val userSession = SessionManager(initialAnonymousId = "anon_user_sample_$i")
            val engine = ExperimentEngine(userSession, FakeUserPreferences())
            val variant = engine.getVariant(expId, variants)
            userResults.add(variant)
        }

        // Across 20 distinct users, both variants should be represented
        assertTrue(userResults.contains("monthly_first"))
        assertTrue(userResults.contains("yearly_first"))
    }

    @Test
    fun overrideVariant_takesPrecedenceOverDeterministicAllocation() = runTest {
        val expId = "checkout_cta_color"
        val variants = listOf("control", "variant_blue", "variant_green")

        val naturalVariant = experimentEngine.getVariant(expId, variants)

        // Force a specific override variant
        val targetOverride = if (naturalVariant == "variant_blue") "variant_green" else "variant_blue"
        experimentEngine.overrideVariant(expId, targetOverride)

        val resolvedVariant = experimentEngine.getVariant(expId, variants)
        assertEquals(targetOverride, resolvedVariant)
    }

    @Test
    fun overrideVariant_withNullOrBlank_removesOverride() = runTest {
        val expId = "nav_redesign"
        val variants = listOf("control", "variant_v2")

        val naturalVariant = experimentEngine.getVariant(expId, variants)

        // Override to something else
        val forced = if (naturalVariant == "control") "variant_v2" else "control"
        experimentEngine.overrideVariant(expId, forced)
        assertEquals(forced, experimentEngine.getVariant(expId, variants))

        // Clear individual override
        experimentEngine.overrideVariant(expId, null)
        assertEquals(naturalVariant, experimentEngine.getVariant(expId, variants))
    }

    @Test
    fun clearOverrides_removesAllConfiguredOverrides() = runTest {
        val exp1 = "exp_one"
        val exp2 = "exp_two"

        experimentEngine.overrideVariant(exp1, "forced_1")
        experimentEngine.overrideVariant(exp2, "forced_2")

        assertEquals("forced_1", experimentEngine.getVariant(exp1))
        assertEquals("forced_2", experimentEngine.getVariant(exp2))

        experimentEngine.clearOverrides()

        // Overrides should no longer be returned
        assertNotEquals("forced_1", experimentEngine.getVariant(exp1))
        assertNotEquals("forced_2", experimentEngine.getVariant(exp2))
    }

    @Test
    fun getVariant_withEmptyVariants_returnsControlDefault() = runTest {
        val variant = experimentEngine.getVariant("empty_exp", emptyList())
        assertEquals("control", variant)
    }
}
