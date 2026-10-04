package com.kotlinfoundation.koko.core.config

/**
 * Environment-specific configuration settings.
 */
data class AppEnvironmentConfig(
    val environment: Environment = Environment.DEV,
    val backendUrl: String = "",
    val isLoggingEnabled: Boolean = true,
    val apiTimeoutMillis: Long = 60_000L,
) {
    companion object {
        fun forEnvironment(
            environment: Environment,
            backendUrl: String = "",
        ): AppEnvironmentConfig = when (environment) {
            Environment.DEV -> AppEnvironmentConfig(
                environment = Environment.DEV,
                backendUrl = backendUrl,
                isLoggingEnabled = true,
                apiTimeoutMillis = 60_000L,
            )

            Environment.STAGING -> AppEnvironmentConfig(
                environment = Environment.STAGING,
                backendUrl = backendUrl,
                isLoggingEnabled = true,
                apiTimeoutMillis = 45_000L,
            )

            Environment.PRODUCTION -> AppEnvironmentConfig(
                environment = Environment.PRODUCTION,
                backendUrl = backendUrl,
                isLoggingEnabled = false,
                apiTimeoutMillis = 30_000L,
            )
        }
    }
}
