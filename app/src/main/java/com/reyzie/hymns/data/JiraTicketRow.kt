package com.reyzie.hymns.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JiraTicketRow(
    val id: String,
    @SerialName("ticket_key") val ticketKey: String,
    @SerialName("ticket_url") val ticketUrl: String,
    @SerialName("song_type") val songType: String,
    @SerialName("song_number") val songNumber: Int,
    @SerialName("song_title") val songTitle: String,
    val description: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("jira_status") val jiraStatus: String = "Open",
    @SerialName("jira_status_id") val jiraStatusId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)
