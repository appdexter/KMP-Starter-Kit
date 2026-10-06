package com.kotlinfoundation.koko.core.consent

/**
 * Snapshot of user consent state.
 *
 * @property analyticsGranted Whether analytics tracking is consented.
 * @property adsGranted Whether general advertising is consented.
 * @property personalizedAdsGranted Whether personalized/targeted ads are consented.
 * @property attStatus iOS App Tracking Transparency status (or equivalent tracking status).
 */
data class ConsentState(
    val analyticsGranted: Boolean = true,
    val adsGranted: Boolean = true,
    val personalizedAdsGranted: Boolean = true,
    val attStatus: AttStatus = AttStatus.NOT_DETERMINED,
) {
    fun hasConsent(type: ConsentType): Boolean = when (type) {
        ConsentType.ANALYTICS -> analyticsGranted
        ConsentType.ADS -> adsGranted
        ConsentType.ADS_PERSONALIZATION -> adsGranted && personalizedAdsGranted
        ConsentType.ATT -> attStatus == AttStatus.AUTHORIZED
    }
}
