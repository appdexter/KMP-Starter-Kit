package com.kotlinfoundation.koko.data.source.remote.apiservices.attribution

import com.kotlinfoundation.koko.data.source.remote.request.attribution.AttributionSyncRequest
import com.kotlinfoundation.koko.data.source.remote.response.attribution.AttributionSyncResponse
import com.kotlinfoundation.koko.root.AppConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Remote API service for communicating with the Cloudflare Edge Backend to sync attribution data.
 */
class AttributionApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String = AppConfiguration.CLOUDFLARE_BACKEND_URL,
) {

    suspend fun syncAttribution(request: AttributionSyncRequest): AttributionSyncResponse {
        val cleanBaseUrl = baseUrl.trim().trimEnd('/')
        if (cleanBaseUrl.isBlank()) {
            return AttributionSyncResponse(
                success = false,
                message = "Backend URL not configured",
                appUserId = request.appUserId,
            )
        }

        return httpClient.post("$cleanBaseUrl/api/v1/attribution/sync") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
