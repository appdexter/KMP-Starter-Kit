package com.kotlinfoundation.koko.core.consent

/**
 * Types of user consent tracked across the application.
 */
enum class ConsentType {
    ANALYTICS,
    ADS,
    ADS_PERSONALIZATION,
    ATT,
}

/**
 * Platform App Tracking Transparency (ATT) or advertising tracking authorization status.
 */
enum class AttStatus {
    NOT_DETERMINED,
    RESTRICTED,
    DENIED,
    AUTHORIZED,
}
