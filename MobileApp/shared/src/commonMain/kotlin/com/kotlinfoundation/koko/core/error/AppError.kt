package com.kotlinfoundation.koko.core.error

import com.kotlinfoundation.koko.domain.exceptions.CreditRequiredException
import com.kotlinfoundation.koko.domain.exceptions.PurchaseRequiredException
import com.kotlinfoundation.koko.domain.exceptions.UnAuthorizedException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpHeaders
import kotlin.coroutines.cancellation.CancellationException

/**
 * Uniform application-level error representation across network, auth, storage,
 * and billing boundaries.
 */
sealed interface AppError {
    val code: String
    val message: String

    data class Network(
        val httpCode: Int? = null,
        override val message: String,
        override val code: String = "NETWORK_ERROR",
    ) : AppError

    data class Unauthorized(
        override val message: String = "Authentication required",
        override val code: String = "AUTH_REQUIRED",
    ) : AppError

    data class RateLimited(
        val retryAfterSeconds: Int? = null,
        override val message: String = "Rate limited",
        override val code: String = "RATE_LIMITED",
    ) : AppError

    data class Server(
        val httpCode: Int = 500,
        override val message: String,
        override val code: String = "SERVER_ERROR",
    ) : AppError

    data class Storage(
        override val message: String,
        override val code: String = "STORAGE_ERROR",
    ) : AppError

    data class Purchase(
        val reason: String,
        override val message: String = "PURCHASE_FAILED",
        override val code: String = "PURCHASE_FAILED",
    ) : AppError

    data class Unknown(
        val cause: Throwable? = null,
        override val message: String = "UNKNOWN_ERROR",
        override val code: String = "UNKNOWN_ERROR",
    ) : AppError
}

/**
 * Maps any [Throwable] to a strongly typed [AppError].
 */
fun Throwable.toAppError(): AppError = when (this) {
    is UnAuthorizedException -> AppError.Unauthorized(
        message = message ?: "Authentication required",
    )

    is PurchaseRequiredException -> AppError.Purchase(
        reason = message ?: "Purchase required",
        message = message ?: "Purchase required",
    )

    is CreditRequiredException -> AppError.Purchase(
        reason = message ?: "Credit required",
        message = message ?: "Credit required",
    )

    is ClientRequestException -> {
        val status = response.status.value
        when (status) {
            401, 403 -> AppError.Unauthorized(message = message)

            429 -> {
                val retryAfter = response.headers[HttpHeaders.RetryAfter]?.toIntOrNull()
                AppError.RateLimited(
                    retryAfterSeconds = retryAfter,
                    message = message,
                )
            }

            else -> AppError.Network(httpCode = status, message = message)
        }
    }

    is ServerResponseException -> {
        AppError.Server(httpCode = response.status.value, message = message)
    }

    is ResponseException -> {
        val status = response.status.value
        if (status >= 500) {
            AppError.Server(httpCode = status, message = message ?: "Server error")
        } else {
            AppError.Network(httpCode = status, message = message ?: "Network error")
        }
    }

    is HttpRequestTimeoutException -> {
        AppError.Network(httpCode = null, message = message ?: "Request timed out")
    }

    is CancellationException -> {
        AppError.Unknown(cause = this, message = "Operation cancelled", code = "CANCELLED")
    }

    else -> AppError.Unknown(
        cause = this,
        message = message ?: "Unknown error occurred",
    )
}
