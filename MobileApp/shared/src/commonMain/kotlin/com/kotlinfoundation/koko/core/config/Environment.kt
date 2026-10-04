package com.kotlinfoundation.koko.core.config

/**
 * Deployment environments for the application.
 */
enum class Environment {
    DEV,
    STAGING,
    PRODUCTION,
    ;

    val isDev: Boolean get() = this == DEV
    val isStaging: Boolean get() = this == STAGING
    val isProd: Boolean get() = this == PRODUCTION
}
