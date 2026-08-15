package com.reyzie.hymns.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuestTicketsRpcParams(
    @SerialName("p_device_id") val pDeviceId: String
)

@Serializable
data class GuestTicketMessagesRpcParams(
    @SerialName("p_device_id") val pDeviceId: String,
    @SerialName("p_ticket_key") val pTicketKey: String
)

@Serializable
data class GuestTicketStatusRpcParams(
    @SerialName("p_device_id") val pDeviceId: String,
    @SerialName("p_ticket_key") val pTicketKey: String,
    @SerialName("p_jira_status") val pJiraStatus: String,
    @SerialName("p_jira_status_id") val pJiraStatusId: String
)
