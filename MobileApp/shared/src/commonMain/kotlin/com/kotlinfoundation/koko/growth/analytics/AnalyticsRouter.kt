package com.kotlinfoundation.koko.growth.analytics

import com.kotlinfoundation.koko.util.analytics.Analytics
import com.kotlinfoundation.koko.util.logging.AppLogger

/**
 * Multi-destination router implementing [Analytics].
 * Fans out events to [primaryAnalytics] and any registered [AnalyticsDestination]s.
 * Optionally logs events via [AppLogger.d] when [isLoggingEnabled] is true.
 */
class AnalyticsRouter(
    private val primaryAnalytics: Analytics,
    destinations: List<AnalyticsDestination> = emptyList(),
    var isLoggingEnabled: Boolean = false,
) : Analytics {

    private val _destinations = destinations.toMutableList()
    val destinations: List<AnalyticsDestination>
        get() = _destinations.toList()

    fun registerDestination(destination: AnalyticsDestination) {
        if (!_destinations.contains(destination)) {
            _destinations.add(destination)
        }
    }

    override fun logEvent(event: String, params: Map<String, Any>?) {
        primaryAnalytics.logEvent(event, params)
        _destinations.forEach { destination ->
            try {
                destination.logEvent(event, params)
            } catch (e: Throwable) {
                safeLogError("AnalyticsRouter failed to dispatch to destination '${destination.name}': ${e.message}", e)
            }
        }
        if (isLoggingEnabled) {
            safeLogDebug("AnalyticsRouter [$event]: $params")
        }
    }

    override fun setEnabled(enabled: Boolean) {
        primaryAnalytics.setEnabled(enabled)
    }

    private fun safeLogDebug(message: String) {
        try {
            AppLogger.d(message)
        } catch (_: Throwable) {
            // Ignored when logging subsystem or Koin is uninitialized (e.g. in headless unit tests)
        }
    }

    private fun safeLogError(message: String, throwable: Throwable?) {
        try {
            AppLogger.e(message, throwable)
        } catch (_: Throwable) {
            // Ignored when logging subsystem or Koin is uninitialized (e.g. in headless unit tests)
        }
    }
}
