package com.kotlinfoundation.koko.core.consent

import com.kotlinfoundation.koko.data.source.preferences.FakeUserPreferences
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsentManagerTest {

    private lateinit var userPreferences: FakeUserPreferences
    private lateinit var consentManager: ConsentManager

    @BeforeTest
    fun setUp() {
        userPreferences = FakeUserPreferences()
        consentManager = ConsentManager(userPreferences)
    }

    @Test
    fun initialDefaultState_hasCorrectDefaults() {
        val state = consentManager.consentState.value

        assertTrue(state.analyticsGranted)
        assertTrue(state.adsGranted)
        assertTrue(state.personalizedAdsGranted)
        assertEquals(AttStatus.NOT_DETERMINED, state.attStatus)

        assertTrue(consentManager.hasConsent(ConsentType.ANALYTICS))
        assertTrue(consentManager.hasConsent(ConsentType.ADS))
        assertTrue(consentManager.hasConsent(ConsentType.ADS_PERSONALIZATION))
        // ATT consent requires AUTHORIZED status
        assertFalse(consentManager.hasConsent(ConsentType.ATT))
    }

    @Test
    fun updateConsent_analytics_updatesStateAndHasConsent() = runTest {
        consentManager.updateConsent(ConsentType.ANALYTICS, false)

        assertFalse(consentManager.consentState.value.analyticsGranted)
        assertFalse(consentManager.hasConsent(ConsentType.ANALYTICS))

        consentManager.updateConsent(ConsentType.ANALYTICS, true)

        assertTrue(consentManager.consentState.value.analyticsGranted)
        assertTrue(consentManager.hasConsent(ConsentType.ANALYTICS))
    }

    @Test
    fun updateConsent_ads_updatesStateAndAffectsPersonalization() = runTest {
        consentManager.updateConsent(ConsentType.ADS, false)

        assertFalse(consentManager.consentState.value.adsGranted)
        assertFalse(consentManager.hasConsent(ConsentType.ADS))
        // Personalized ads requires ads consent to be granted
        assertFalse(consentManager.hasConsent(ConsentType.ADS_PERSONALIZATION))

        consentManager.updateConsent(ConsentType.ADS, true)

        assertTrue(consentManager.consentState.value.adsGranted)
        assertTrue(consentManager.hasConsent(ConsentType.ADS))
        assertTrue(consentManager.hasConsent(ConsentType.ADS_PERSONALIZATION))
    }

    @Test
    fun updateConsent_adsPersonalization_updatesOnlyPersonalization() = runTest {
        consentManager.updateConsent(ConsentType.ADS_PERSONALIZATION, false)

        assertFalse(consentManager.consentState.value.personalizedAdsGranted)
        assertFalse(consentManager.hasConsent(ConsentType.ADS_PERSONALIZATION))
        // General ads consent remains true
        assertTrue(consentManager.hasConsent(ConsentType.ADS))

        consentManager.updateConsent(ConsentType.ADS_PERSONALIZATION, true)

        assertTrue(consentManager.consentState.value.personalizedAdsGranted)
        assertTrue(consentManager.hasConsent(ConsentType.ADS_PERSONALIZATION))
    }

    @Test
    fun updateConsent_att_updatesAttStatus() = runTest {
        consentManager.updateConsent(ConsentType.ATT, true)

        assertEquals(AttStatus.AUTHORIZED, consentManager.consentState.value.attStatus)
        assertTrue(consentManager.hasConsent(ConsentType.ATT))

        consentManager.updateConsent(ConsentType.ATT, false)

        assertEquals(AttStatus.DENIED, consentManager.consentState.value.attStatus)
        assertFalse(consentManager.hasConsent(ConsentType.ATT))
    }

    @Test
    fun updateAttStatus_setsStatusCorrectly() = runTest {
        consentManager.updateAttStatus(AttStatus.AUTHORIZED)
        assertEquals(AttStatus.AUTHORIZED, consentManager.consentState.value.attStatus)
        assertTrue(consentManager.hasConsent(ConsentType.ATT))

        consentManager.updateAttStatus(AttStatus.RESTRICTED)
        assertEquals(AttStatus.RESTRICTED, consentManager.consentState.value.attStatus)
        assertFalse(consentManager.hasConsent(ConsentType.ATT))

        consentManager.updateAttStatus(AttStatus.DENIED)
        assertEquals(AttStatus.DENIED, consentManager.consentState.value.attStatus)
        assertFalse(consentManager.hasConsent(ConsentType.ATT))
    }

    @Test
    fun grantAll_enablesAllConsentsAndAuthorizesAtt() = runTest {
        consentManager.revokeAll()

        assertFalse(consentManager.hasConsent(ConsentType.ANALYTICS))
        assertFalse(consentManager.hasConsent(ConsentType.ADS))
        assertFalse(consentManager.hasConsent(ConsentType.ADS_PERSONALIZATION))
        assertFalse(consentManager.hasConsent(ConsentType.ATT))

        consentManager.grantAll()

        val state = consentManager.consentState.value
        assertTrue(state.analyticsGranted)
        assertTrue(state.adsGranted)
        assertTrue(state.personalizedAdsGranted)
        assertEquals(AttStatus.AUTHORIZED, state.attStatus)

        assertTrue(consentManager.hasConsent(ConsentType.ANALYTICS))
        assertTrue(consentManager.hasConsent(ConsentType.ADS))
        assertTrue(consentManager.hasConsent(ConsentType.ADS_PERSONALIZATION))
        assertTrue(consentManager.hasConsent(ConsentType.ATT))
    }

    @Test
    fun revokeAll_disablesAllConsentsAndDeniesAtt() = runTest {
        consentManager.revokeAll()

        val state = consentManager.consentState.value
        assertFalse(state.analyticsGranted)
        assertFalse(state.adsGranted)
        assertFalse(state.personalizedAdsGranted)
        assertEquals(AttStatus.DENIED, state.attStatus)

        assertFalse(consentManager.hasConsent(ConsentType.ANALYTICS))
        assertFalse(consentManager.hasConsent(ConsentType.ADS))
        assertFalse(consentManager.hasConsent(ConsentType.ADS_PERSONALIZATION))
        assertFalse(consentManager.hasConsent(ConsentType.ATT))
    }

    @Test
    fun persistenceAndReloadWithFakeUserPreferences() = runTest {
        consentManager.updateConsent(ConsentType.ANALYTICS, false)
        consentManager.updateConsent(ConsentType.ADS, false)
        consentManager.updateConsent(ConsentType.ADS_PERSONALIZATION, false)
        consentManager.updateAttStatus(AttStatus.RESTRICTED)

        // Verify direct preference persistence
        assertEquals(false, userPreferences.getBoolean(ConsentManager.KEY_CONSENT_ANALYTICS))
        assertEquals(false, userPreferences.getBoolean(ConsentManager.KEY_CONSENT_ADS))
        assertEquals(false, userPreferences.getBoolean(ConsentManager.KEY_CONSENT_PERSONALIZED_ADS))
        assertEquals(AttStatus.RESTRICTED.name, userPreferences.getString(ConsentManager.KEY_CONSENT_ATT_STATUS))

        // Reload via new instance with the same preferences
        val reloadedManager = ConsentManager(userPreferences)
        reloadedManager.loadConsent()

        assertEquals(false, reloadedManager.consentState.value.analyticsGranted)
        assertEquals(false, reloadedManager.consentState.value.adsGranted)
        assertEquals(false, reloadedManager.consentState.value.personalizedAdsGranted)
        assertEquals(AttStatus.RESTRICTED, reloadedManager.consentState.value.attStatus)

        assertFalse(reloadedManager.hasConsent(ConsentType.ANALYTICS))
        assertFalse(reloadedManager.hasConsent(ConsentType.ADS))
        assertFalse(reloadedManager.hasConsent(ConsentType.ADS_PERSONALIZATION))
        assertFalse(reloadedManager.hasConsent(ConsentType.ATT))

        // Also test factory method ConsentManager.create()
        val createdManager = ConsentManager.create(userPreferences)
        assertEquals(false, createdManager.hasConsent(ConsentType.ANALYTICS))
        assertEquals(AttStatus.RESTRICTED, createdManager.consentState.value.attStatus)
    }
}
