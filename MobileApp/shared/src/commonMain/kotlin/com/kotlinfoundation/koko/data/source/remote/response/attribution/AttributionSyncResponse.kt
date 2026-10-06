package com.kotlinfoundation.koko.data.source.remote.response.attribution

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttributionSyncResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String? = null,
    @SerialName("app_user_id") val appUserId: String? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
)
