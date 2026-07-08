package com.reyzie.hymns.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InAppMessage(
    val id: String,
    val title: String,
    val message: String,
    @SerialName("action_text") val actionText: String? = null,
    @SerialName("action_url") val actionUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String
)

val InAppMessage.displayMessage: String
    get() {
        val index = message.indexOf("||image_url=")
        return if (index != -1) message.substring(0, index).trim() else message
    }

val InAppMessage.imageUrl: String?
    get() {
        val marker = "||image_url="
        val index = message.indexOf(marker)
        return if (index != -1) message.substring(index + marker.length).trim() else null
    }
