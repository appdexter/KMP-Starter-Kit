package com.kotlinfoundation.koko.identity

import com.kotlinfoundation.koko.identity.model.UserIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserIdentityTest {

    @Test
    fun defaultValues_areInitializedProperly() {
        val identity = UserIdentity(anonymousId = "anon-123")
        assertEquals("anon-123", identity.anonymousId)
        assertNull(identity.firebaseUid)
        assertNull(identity.attributionId)
        assertFalse(identity.isPremium)
        assertEquals("", identity.appVersion)
        assertEquals("", identity.platform)
    }

    @Test
    fun copyWithUpdatedFields_updatesCorrectly() {
        val identity = UserIdentity(anonymousId = "anon-123")
        val updated = identity.copy(
            firebaseUid = "uid-456",
            attributionId = "attr-789",
            isPremium = true,
            appVersion = "1.0.0",
            platform = "Android",
        )

        assertEquals("anon-123", updated.anonymousId)
        assertEquals("uid-456", updated.firebaseUid)
        assertEquals("attr-789", updated.attributionId)
        assertTrue(updated.isPremium)
        assertEquals("1.0.0", updated.appVersion)
        assertEquals("Android", updated.platform)
    }
}
