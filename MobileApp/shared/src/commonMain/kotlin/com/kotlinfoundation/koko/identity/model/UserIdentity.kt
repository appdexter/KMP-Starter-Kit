package com.kotlinfoundation.koko.identity.model

/**
 * Identity state of the current user session across anonymous, authenticated,
 * and attribution boundaries.
 */
data class UserIdentity(
    val anonymousId: String,
    val firebaseUid: String? = null,
    val attributionId: String? = null,
    val isPremium: Boolean = false,
    val appVersion: String = "",
    val platform: String = "",
)
