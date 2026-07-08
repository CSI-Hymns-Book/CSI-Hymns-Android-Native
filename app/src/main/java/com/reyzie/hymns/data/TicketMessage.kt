package com.reyzie.hymns.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TicketMessage(
    val id: String? = null,
    @SerialName("ticket_id") val ticketId: String,
    @SerialName("ticket_key") val ticketKey: String,
    val sender: String, // "user" or "admin"
    val message: String,
    @SerialName("created_at") val createdAt: String? = null
)
