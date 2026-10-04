package com.kotlinfoundation.koko.identity

import com.kotlinfoundation.koko.data.repository.UserRepository
import com.kotlinfoundation.koko.data.source.preferences.UserPreferences
import com.kotlinfoundation.koko.identity.model.UserIdentity
import com.kotlinfoundation.koko.root.AppConfiguration
import com.kotlinfoundation.koko.util.ApplicationScope
import com.kotlinfoundation.koko.util.getPlatform
import com.kotlinfoundation.koko.util.logging.AppLogger
import com.mmk.kmpauth.core.KMPAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Manages user session state, anonymous ID persistence, and identity observation.
 */
open class SessionManager(
    private val userPreferences: UserPreferences? = null,
    private val userRepositoryProvider: (() -> UserRepository)? = null,
    private val applicationScope: ApplicationScope? = null,
    initialAnonymousId: String? = null,
) {
    @OptIn(ExperimentalUuidApi::class)
    protected var cachedAnonymousId: String = initialAnonymousId ?: Uuid.random().toString()

    constructor(
        userPreferences: UserPreferences?,
        userRepository: UserRepository?,
        applicationScope: ApplicationScope?,
        initialAnonymousId: String? = null,
    ) : this(
        userPreferences = userPreferences,
        userRepositoryProvider = userRepository?.let { { it } },
        applicationScope = applicationScope,
        initialAnonymousId = initialAnonymousId,
    )

    constructor(initialAnonymousId: String) : this(
        userPreferences = null,
        userRepositoryProvider = null,
        applicationScope = null,
        initialAnonymousId = initialAnonymousId,
    )

    private val _userIdentity = MutableStateFlow(
        UserIdentity(
            anonymousId = cachedAnonymousId,
            firebaseUid = if (AppConfiguration.isAuthEnabled) {
                try {
                    KMPAuth.currentUser()?.uid
                } catch (_: Throwable) {
                    null
                }
            } else {
                null
            },
            isPremium = false,
            appVersion = "1.0.0",
            platform = try {
                getPlatform().toString()
            } catch (_: Throwable) {
                ""
            },
        ),
    )
    val userIdentity: StateFlow<UserIdentity> = _userIdentity.asStateFlow()

    init {
        if (userPreferences != null && userRepositoryProvider != null && applicationScope != null) {
            initializeSession(userPreferences, userRepositoryProvider, applicationScope)
        }
    }

    private fun initializeSession(
        userPreferences: UserPreferences,
        userRepositoryProvider: () -> UserRepository,
        applicationScope: ApplicationScope,
    ) {
        applicationScope.launch {
            try {
                val storedId = userPreferences.getString(UserPreferences.KEY_ANONYMOUS_ID)
                if (storedId.isNullOrBlank()) {
                    userPreferences.putString(UserPreferences.KEY_ANONYMOUS_ID, cachedAnonymousId)
                } else {
                    cachedAnonymousId = storedId
                }
                _userIdentity.update { it.copy(anonymousId = cachedAnonymousId) }
            } catch (e: Exception) {
                AppLogger.e("SessionManager failed to load or persist anonymousId: ${e.message}")
            }
        }

        applicationScope.launch {
            val userRepo = userRepositoryProvider()
            userRepo.currentUser.collect { userResult ->
                val user = userResult.getOrNull()
                _userIdentity.update { current ->
                    current.copy(
                        anonymousId = cachedAnonymousId,
                        firebaseUid = if (AppConfiguration.isAuthEnabled) {
                            user?.id ?: try {
                                KMPAuth.currentUser()?.uid
                            } catch (_: Throwable) {
                                null
                            }
                        } else {
                            null
                        },
                        isPremium = user?.hasPremiumAccess ?: false,
                    )
                }
            }
        }
    }

    /**
     * Synchronously returns the currently cached anonymous ID.
     */
    open fun getAnonymousId(): String = cachedAnonymousId

    /**
     * Retrieves the Firebase ID token for authenticated requests, or null if not signed in.
     */
    suspend fun getUserIdToken(forceRefresh: Boolean = false): String? {
        if (!AppConfiguration.isAuthEnabled) return null
        return KMPAuth.currentUserIdToken(forceRefresh = forceRefresh).getOrNull()
    }

    /**
     * Updates attribution identifier (e.g. from campaign attribution or deep links).
     */
    fun updateAttributionId(attributionId: String?) {
        _userIdentity.update { it.copy(attributionId = attributionId) }
    }

    /**
     * Updates the premium status in the current user session.
     */
    fun setPremium(isPremium: Boolean) {
        _userIdentity.update { it.copy(isPremium = isPremium) }
    }
}
