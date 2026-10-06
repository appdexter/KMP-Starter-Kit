package com.kotlinfoundation.koko.data.source.remote.request.attribution

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttributionSyncRequest(
    @SerialName("app_user_id") val appUserId: String,
    @SerialName("fbclid") val fbclid: String? = null,
    @SerialName("gclid") val gclid: String? = null,
    @SerialName("fbp") val fbp: String? = null,
    @SerialName("fbc") val fbc: String? = null,
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("user_agent") val userAgent: String? = null,
)
