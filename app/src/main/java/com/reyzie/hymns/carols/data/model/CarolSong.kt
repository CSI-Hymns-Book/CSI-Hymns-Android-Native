package com.reyzie.hymns.carols.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CarolSong(
    val id: String,
    @SerialName("church_id") val churchId: String,
    val title: String,
    @SerialName("song_number") val songNumber: String? = null,
    val lyrics: String,
    val scale: String = "C Major",
    @SerialName("created_by_user_id") val createdByUserId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)
