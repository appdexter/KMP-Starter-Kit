package com.kotlinfoundation.koko.data.repository

import com.kotlinfoundation.koko.data.BackgroundExecutor
import com.kotlinfoundation.koko.data.source.remote.apiservices.attribution.AttributionApiService
import com.kotlinfoundation.koko.data.source.remote.request.attribution.AttributionSyncRequest
import com.kotlinfoundation.koko.data.source.remote.response.attribution.AttributionSyncResponse

/**
 * Concrete repository that manages client-side attribution state (fbclid, gclid, Meta cookies)
 * and synchronizes it with the Cloudflare Edge Backend.
 */
class AttributionRepository(
    private val attributionApiService: AttributionApiService,
    private val backgroundExecutor: BackgroundExecutor = BackgroundExecutor.IO,
) {
    private var fbclid: String? = null
    private var gclid: String? = null
    private var fbp: String? = null
    private var fbc: String? = null
    private var ipAddress: String? = null
    private var userAgent: String? = null

    /**
     * Cache attribution query parameters and cookies captured from deep links, web views, or app launch.
     */
    fun setAttributionParams(
        fbclid: String? = null,
        gclid: String? = null,
        fbp: String? = null,
        fbc: String? = null,
    ) {
        if (fbclid != null) this.fbclid = fbclid
        if (gclid != null) this.gclid = gclid
        if (fbp != null) this.fbp = fbp
        if (fbc != null) this.fbc = fbc
    }

    /**
     * Cache client metadata (IP address and user agent) if available on the client platform.
     */
    fun setClientMetadata(
        ipAddress: String? = null,
        userAgent: String? = null,
    ) {
        if (ipAddress != null) this.ipAddress = ipAddress
        if (userAgent != null) this.userAgent = userAgent
    }

    /**
     * Get a snapshot of the currently cached attribution parameters.
     */
    fun currentParams(): AttributionSyncRequest = AttributionSyncRequest(
        appUserId = "",
        fbclid = fbclid,
        gclid = gclid,
        fbp = fbp,
        fbc = fbc,
        ipAddress = ipAddress,
        userAgent = userAgent,
    )

    /**
     * Clears all cached attribution parameters and client metadata.
     */
    fun clear() {
        fbclid = null
        gclid = null
        fbp = null
        fbc = null
        ipAddress = null
        userAgent = null
    }

    /**
     * Synchronize the cached attribution parameters for the given [appUserId] to the Cloudflare Edge Backend.
     */
    suspend fun sync(appUserId: String): Result<AttributionSyncResponse> = backgroundExecutor.execute {
        val request = AttributionSyncRequest(
            appUserId = appUserId,
            fbclid = fbclid,
            gclid = gclid,
            fbp = fbp,
            fbc = fbc,
            ipAddress = ipAddress,
            userAgent = userAgent,
        )
        val response = attributionApiService.syncAttribution(request)
        Result.success(response)
    }
}
