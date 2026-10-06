package com.kotlinfoundation.koko.core.consent

import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages user consent state across analytics, ads, personalized ads, and App Tracking Transparency (ATT).
 * Persists consent decisions into [UserPreferences].
 */
class ConsentManager(
    private val userPreferences: UserPreferences? = null,
    coroutineScope: CoroutineScope? = null,
) {
    private val _consentState = MutableStateFlow(ConsentState())
    val consentState: StateFlow<ConsentState> = _consentState.asStateFlow()

    init {
        coroutineScope?.launch {
            loadConsent()
        }
    }

    fun hasConsent(type: ConsentType): Boolean = _consentState.value.hasConsent(type)

    suspend fun updateConsent(type: ConsentType, granted: Boolean) {
        val current = _consentState.value
        val updated = when (type) {
            ConsentType.ANALYTICS -> current.copy(analyticsGranted = granted)

            ConsentType.ADS -> current.copy(adsGranted = granted)

            ConsentType.ADS_PERSONALIZATION -> current.copy(personalizedAdsGranted = granted)

            ConsentType.ATT -> current.copy(
                attStatus = if (granted) AttStatus.AUTHORIZED else AttStatus.DENIED,
            )
        }
        _consentState.value = updated
        persistState(updated)
    }

    suspend fun updateAttStatus(status: AttStatus) {
        val updated = _consentState.value.copy(attStatus = status)
        _consentState.value = updated
        persistState(updated)
    }

    suspend fun grantAll() {
        val updated = _consentState.value.copy(
            analyticsGranted = true,
            adsGranted = true,
            personalizedAdsGranted = true,
            attStatus = AttStatus.AUTHORIZED,
        )
        _consentState.value = updated
        persistState(updated)
    }

    suspend fun revokeAll() {
        val updated = _consentState.value.copy(
            analyticsGranted = false,
            adsGranted = false,
            personalizedAdsGranted = false,
            attStatus = AttStatus.DENIED,
        )
        _consentState.value = updated
        persistState(updated)
    }

    suspend fun loadConsent(): ConsentState {
        val prefs = userPreferences ?: return _consentState.value
        val analytics = prefs.getBoolean(KEY_CONSENT_ANALYTICS, defaultValue = true)
        val ads = prefs.getBoolean(KEY_CONSENT_ADS, defaultValue = true)
        val personalizedAds = prefs.getBoolean(KEY_CONSENT_PERSONALIZED_ADS, defaultValue = true)
        val attStatusStr = prefs.getString(KEY_CONSENT_ATT_STATUS, AttStatus.NOT_DETERMINED.name)
        val attStatus = try {
            attStatusStr?.let { AttStatus.valueOf(it) } ?: AttStatus.NOT_DETERMINED
        } catch (_: Exception) {
            AttStatus.NOT_DETERMINED
        }
        val loaded = ConsentState(
            analyticsGranted = analytics,
            adsGranted = ads,
            personalizedAdsGranted = personalizedAds,
            attStatus = attStatus,
        )
        _consentState.value = loaded
        return loaded
    }

    suspend fun reload(): ConsentState = loadConsent()

    private suspend fun persistState(state: ConsentState) {
        userPreferences?.apply {
            putBoolean(KEY_CONSENT_ANALYTICS, state.analyticsGranted)
            putBoolean(KEY_CONSENT_ADS, state.adsGranted)
            putBoolean(KEY_CONSENT_PERSONALIZED_ADS, state.personalizedAdsGranted)
            putString(KEY_CONSENT_ATT_STATUS, state.attStatus.name)
        }
    }

    companion object {
        const val KEY_CONSENT_ANALYTICS = "consent_analytics"
        const val KEY_CONSENT_ADS = "consent_ads"
        const val KEY_CONSENT_PERSONALIZED_ADS = "consent_personalized_ads"
        const val KEY_CONSENT_ATT_STATUS = "consent_att_status"

        suspend fun create(userPreferences: UserPreferences? = null): ConsentManager {
            val manager = ConsentManager(userPreferences)
            manager.loadConsent()
            return manager
        }
    }
}
