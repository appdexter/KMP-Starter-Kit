package com.kotlinfoundation.koko.data.source.preferences

/**
 * Simple key/value local storage (backed by DataStore Preferences). For small primitive flags
 * and counters — not for structured data, which belongs in Room. Keys live in [Keys].
 */
interface UserPreferences {

    companion object Keys {
        const val KEY_IS_ONBOARD_SHOWN = "KEY_IS_ONBOARD_SHOWN"
        const val KEY_FIRST_TIME_USER = "KEY_FIRST_TIME_USER"
        const val KEY_NB_PAYWALL_DISMISSED = "KEY_NB_PAYWALL_DISMISSED"
        const val KEY_USER_GOAL = "KEY_USER_GOAL"
        const val KEY_USER_BARRIER = "KEY_USER_BARRIER"
        const val KEY_DAILY_COMMITMENT = "KEY_DAILY_COMMITMENT"
        const val KEY_HAS_SEEN_EXIT_DOWNSELL = "KEY_HAS_SEEN_EXIT_DOWNSELL"
        const val KEY_DOWNSELL_START_TIME_MILLIS = "KEY_DOWNSELL_START_TIME_MILLIS"
        const val KEY_ANONYMOUS_ID = "KEY_ANONYMOUS_ID"
        const val KEY_IS_DEV_MODE_ENABLED = "KEY_IS_DEV_MODE_ENABLED"
    }

    suspend fun getString(key: String, defaultValue: String? = null): String?
    suspend fun getInt(key: String, defaultValue: Int? = null): Int?
    suspend fun getLong(key: String, defaultValue: Long? = null): Long?
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    suspend fun putString(key: String, value: String)
    suspend fun putInt(key: String, value: Int)
    suspend fun putLong(key: String, value: Long)
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun remove(key: String)

    suspend fun clear()

    suspend fun hasSeenExitDownsell(): Boolean
    suspend fun setExitDownsellSeen(seen: Boolean)
    suspend fun getDownsellStartTimeMillis(): Long?
    suspend fun setDownsellStartTimeMillis(timeMillis: Long)
}
