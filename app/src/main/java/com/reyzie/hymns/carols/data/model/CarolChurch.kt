package com.reyzie.hymns.carols.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CarolChurch(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("created_by_user_id") val createdByUserId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)
