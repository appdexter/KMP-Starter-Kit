package com.kotlinfoundation.koko.core.navigation

import com.kotlinfoundation.koko.data.repository.AttributionRepository
import com.kotlinfoundation.koko.presentation.navigation.ScreenRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event delivered when a deep link is received and parsed.
 */
data class DeepLinkEvent(
    val route: ScreenRoute,
    val attribution: AttributionQueryData? = null,
)

/**
 * Coordinates receiving, parsing, buffering, and dispatching deep link events.
 */
class DeepLinkManager(
    private val parser: DeepLinkParser = DeepLinkParser(),
    private val attributionRepository: AttributionRepository? = null,
) {
    companion object {
        var defaultInstance: DeepLinkManager? = null

        fun onDeepLinkReceived(uri: String): Boolean = defaultInstance?.onDeepLinkReceived(uri) ?: (DeepLinkParser.parse(uri) != null)
    }

    init {
        defaultInstance = this
    }
    private val _events = MutableSharedFlow<DeepLinkEvent>(
        replay = 1,
        extraBufferCapacity = 64,
    )
    val events: SharedFlow<DeepLinkEvent> = _events.asSharedFlow()

    private var _pendingEvent: DeepLinkEvent? = null

    /**
     * The latest unconsumed deep link event, if any.
     */
    val pendingEvent: DeepLinkEvent?
        get() = _pendingEvent

    /**
     * Consumes and clears the pending deep link event.
     */
    fun consumePendingEvent(): DeepLinkEvent? {
        val event = _pendingEvent
        _pendingEvent = null
        return event
    }

    /**
     * Clears all pending and buffered replay events.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun clearPending() {
        _pendingEvent = null
        _events.resetReplayCache()
    }

    /**
     * Handles an incoming deep link URI.
     *
     * @return true if the URI was recognized and routed, false otherwise.
     */
    fun onDeepLinkReceived(uri: String): Boolean {
        val parsed = parser.parse(uri) ?: return false

        val event = DeepLinkEvent(
            route = parsed.route,
            attribution = parsed.attribution,
        )

        _pendingEvent = event
        _events.tryEmit(event)

        val attr = parsed.attribution
        if (attributionRepository != null && attr != null && attr.hasData) {
            attributionRepository.setAttributionParams(
                fbclid = attr.fbclid,
                gclid = attr.gclid,
                fbp = attr.fbp,
                fbc = attr.fbc,
            )
        }

        return true
    }
}
