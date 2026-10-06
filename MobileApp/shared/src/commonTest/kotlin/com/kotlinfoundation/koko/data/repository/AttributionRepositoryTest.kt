package com.kotlinfoundation.koko.data.repository

import com.kotlinfoundation.koko.data.BackgroundExecutor
import com.kotlinfoundation.koko.data.source.remote.apiservices.attribution.AttributionApiService
import com.kotlinfoundation.koko.data.source.remote.request.attribution.AttributionSyncRequest
import com.kotlinfoundation.koko.data.source.remote.response.attribution.AttributionSyncResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AttributionRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private fun createMockClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    @Test
    fun `sync sends cached attribution params and returns success result`() = runTest {
        var recordedPath: String? = null
        var recordedMethod: HttpMethod? = null
        var recordedBodyText: String? = null

        val engine = MockEngine { request ->
            recordedPath = request.url.encodedPath
            recordedMethod = request.method
            recordedBodyText = request.body.toByteArray().decodeToString()

            respond(
                content = """{"success":true,"message":"Attribution synced","app_user_id":"user_123","updated_at":1728000000}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = createMockClient(engine)
        val apiService = AttributionApiService(client, baseUrl = "https://backend.test")
        val executor = BackgroundExecutor(StandardTestDispatcher(testScheduler))
        val repository = AttributionRepository(apiService, executor)

        repository.setAttributionParams(
            fbclid = "fb_test_123",
            gclid = "g_test_456",
            fbp = "fb.1.12345",
            fbc = "fb.1.67890",
        )
        repository.setClientMetadata(
            ipAddress = "1.2.3.4",
            userAgent = "KokoApp/1.0",
        )

        val result = repository.sync("user_123")

        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertTrue(response.success)
        assertEquals("Attribution synced", response.message)
        assertEquals("user_123", response.appUserId)
        assertEquals(1728000000L, response.updatedAt)

        assertEquals(HttpMethod.Post, recordedMethod)
        assertEquals("/api/v1/attribution/sync", recordedPath)
        assertNotNull(recordedBodyText)
        assertTrue(recordedBodyText!!.contains("user_123"))
        assertTrue(recordedBodyText!!.contains("fb_test_123"))
        assertTrue(recordedBodyText!!.contains("g_test_456"))
        assertTrue(recordedBodyText!!.contains("fb.1.12345"))
        assertTrue(recordedBodyText!!.contains("fb.1.67890"))
        assertTrue(recordedBodyText!!.contains("1.2.3.4"))
        assertTrue(recordedBodyText!!.contains("KokoApp/1.0"))
    }

    @Test
    fun `sync returns fallback response when backend url is blank`() = runTest {
        val engine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = createMockClient(engine)
        val apiService = AttributionApiService(client, baseUrl = "")
        val executor = BackgroundExecutor(StandardTestDispatcher(testScheduler))
        val repository = AttributionRepository(apiService, executor)

        val result = repository.sync("user_fallback")

        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertFalse(response.success)
        assertEquals("Backend URL not configured", response.message)
        assertEquals("user_fallback", response.appUserId)
        assertEquals(0, engine.requestHistory.size)
    }

    @Test
    fun `sync wraps network failure in Result failure`() = runTest {
        val engine = MockEngine {
            respondError(HttpStatusCode.InternalServerError, content = "Internal Error")
        }

        val client = createMockClient(engine)
        val apiService = AttributionApiService(client, baseUrl = "https://backend.test")
        val executor = BackgroundExecutor(StandardTestDispatcher(testScheduler))
        val repository = AttributionRepository(apiService, executor)

        val result = repository.sync("user_fail")

        assertTrue(result.isFailure)
    }

    @Test
    fun `clear resets cached attribution parameters`() {
        val client = createMockClient(MockEngine { respond("{}", HttpStatusCode.OK) })
        val repository = AttributionRepository(AttributionApiService(client))

        repository.setAttributionParams(fbclid = "fb1", gclid = "g1")
        repository.setClientMetadata(ipAddress = "127.0.0.1", userAgent = "TestAgent")

        val before = repository.currentParams()
        assertEquals("fb1", before.fbclid)
        assertEquals("g1", before.gclid)
        assertEquals("127.0.0.1", before.ipAddress)
        assertEquals("TestAgent", before.userAgent)

        repository.clear()

        val after = repository.currentParams()
        assertEquals(null, after.fbclid)
        assertEquals(null, after.gclid)
        assertEquals(null, after.fbp)
        assertEquals(null, after.fbc)
        assertEquals(null, after.ipAddress)
        assertEquals(null, after.userAgent)
    }
}
