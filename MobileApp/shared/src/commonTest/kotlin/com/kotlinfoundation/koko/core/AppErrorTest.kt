package com.kotlinfoundation.koko.core

import com.kotlinfoundation.koko.core.config.AppEnvironmentConfig
import com.kotlinfoundation.koko.core.config.Environment
import com.kotlinfoundation.koko.core.error.AppError
import com.kotlinfoundation.koko.core.error.toAppError
import com.kotlinfoundation.koko.domain.exceptions.PurchaseRequiredException
import com.kotlinfoundation.koko.domain.exceptions.UnAuthorizedException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppErrorTest {

    @Test
    fun environmentProperties_returnExpectedFlags() {
        val dev = Environment.DEV
        assertTrue(dev.isDev)
        assertFalse(dev.isProd)

        val prod = Environment.PRODUCTION
        assertFalse(prod.isDev)
        assertTrue(prod.isProd)

        val config = AppEnvironmentConfig.forEnvironment(Environment.PRODUCTION, "https://api.example.com")
        assertEquals("https://api.example.com", config.backendUrl)
        assertFalse(config.isLoggingEnabled)
        assertEquals(30_000L, config.apiTimeoutMillis)
    }

    @Test
    fun toAppError_mapsUnauthorizedExceptionCorrectly() {
        val ex = UnAuthorizedException()
        val error = ex.toAppError()
        assertTrue(error is AppError.Unauthorized)
        assertEquals("AUTH_REQUIRED", error.code)
    }

    @Test
    fun toAppError_mapsPurchaseRequiredExceptionCorrectly() {
        val ex = PurchaseRequiredException()
        val error = ex.toAppError()
        assertTrue(error is AppError.Purchase)
        assertEquals("PURCHASE_FAILED", error.code)
    }

    @Test
    fun toAppError_mapsCancellationException() {
        val ex = CancellationException("User cancelled")
        val error = ex.toAppError()
        assertTrue(error is AppError.Unknown)
        assertEquals("CANCELLED", error.code)
    }

    @Test
    fun toAppError_mapsGenericExceptionToUnknown() {
        val ex = IllegalStateException("Something broke")
        val error = ex.toAppError()
        assertTrue(error is AppError.Unknown)
        assertEquals("UNKNOWN_ERROR", error.code)
        assertEquals("Something broke", error.message)
    }
}
