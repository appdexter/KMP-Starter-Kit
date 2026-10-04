package com.kotlinfoundation.koko.monetization.entitlement

import com.kotlinfoundation.koko.identity.SessionManager
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntitlementManagerTest {

    private var mockPremiumAccess: Boolean = false
    private var mockCreditBalance: Int = 0
    private lateinit var sessionManager: SessionManager
    private lateinit var entitlementManager: EntitlementManager

    @BeforeTest
    fun setUp() {
        mockPremiumAccess = false
        mockCreditBalance = 0
        sessionManager = SessionManager(initialAnonymousId = "test_user_entitlement_123")
        entitlementManager = EntitlementManager(
            sessionManager = sessionManager,
            premiumAccessChecker = { mockPremiumAccess },
            creditBalanceProvider = { mockCreditBalance },
        )
    }

    @Test
    fun has_premium_returnsTrue_whenSubscriptionRepositoryReportsPremium() = runTest {
        mockPremiumAccess = true
        sessionManager.setPremium(false)

        assertTrue(entitlementManager.has(Entitlement.Premium))
    }

    @Test
    fun has_premium_returnsTrue_whenSessionManagerReportsPremium() = runTest {
        mockPremiumAccess = false
        sessionManager.setPremium(true)

        assertTrue(entitlementManager.has(Entitlement.Premium))
    }

    @Test
    fun has_premium_returnsFalse_whenNeitherReportsPremium() = runTest {
        mockPremiumAccess = false
        sessionManager.setPremium(false)

        assertFalse(entitlementManager.has(Entitlement.Premium))
    }

    @Test
    fun has_lifetime_returnsTrue_whenSubscriptionRepositoryReportsPremium() = runTest {
        mockPremiumAccess = true
        assertTrue(entitlementManager.has(Entitlement.Lifetime))
    }

    @Test
    fun has_lifetime_returnsFalse_whenSubscriptionRepositoryReportsNoPremium() = runTest {
        mockPremiumAccess = false
        assertFalse(entitlementManager.has(Entitlement.Lifetime))
    }

    @Test
    fun has_aiCredits_returnsTrue_whenCreditsEqualOrExceedMinAmount() = runTest {
        mockCreditBalance = 5

        assertTrue(entitlementManager.has(Entitlement.AiCredits(minAmount = 1)))
        assertTrue(entitlementManager.has(Entitlement.AiCredits(minAmount = 5)))
    }

    @Test
    fun has_aiCredits_returnsFalse_whenCreditsAreInsufficient() = runTest {
        mockCreditBalance = 2

        assertFalse(entitlementManager.has(Entitlement.AiCredits(minAmount = 3)))
        assertFalse(entitlementManager.has(Entitlement.AiCredits(minAmount = 10)))
    }

    @Test
    fun has_custom_returnsFalse() = runTest {
        assertFalse(entitlementManager.has(Entitlement.Custom("experimental_filter")))
    }
}
