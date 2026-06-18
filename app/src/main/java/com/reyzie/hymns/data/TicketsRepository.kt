package com.reyzie.hymns.data

import android.content.Context
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

data class JiraTicket(
    val ticketKey: String,
    val ticketUrl: String,
    val songType: String,
    val songNumber: Int,
    val songTitle: String,
    val description: String?,
    val appVersion: String?,
    val jiraStatus: String,
    val jiraStatusId: String?,
    val createdAt: String,
    val updatedAt: String
)

class TicketsRepository(private val context: Context) {
    private val supabase = SupabaseService.getInstance()
    private val jiraService = JiraService()
    private val prefs = context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)

    companion object {
        fun isResolvedStatus(status: String): Boolean {
            val value = status.lowercase().trim()
            return value == "done" || value == "resolved" || value == "closed"
        }
    }

    fun getDeviceIdForGuest(): String = getDeviceId()

    private fun getDeviceId(): String {
        var deviceId = prefs.getString("device_id", null)
        if (deviceId.isNullOrEmpty()) {
            deviceId = "device_${System.currentTimeMillis()}_${UUID.randomUUID()}"
            prefs.edit().putString("device_id", deviceId).apply()
        }
        return deviceId
    }

    suspend fun getMyTickets(): List<JiraTicket> = withContext(Dispatchers.IO) {
        try {
            val user = supabase.currentUser
            val rows = if (user != null) {
                supabase.client.from("jira_tickets")
                    .select {
                        filter { eq("user_id", user.id) }
                    }
                    .decodeList<JiraTicketRow>()
            } else {
                val deviceId = getDeviceId()
                supabase.client.from("jira_tickets")
                    .select {
                        filter { eq("device_id", deviceId) }
                    }
                    .decodeList<JiraTicketRow>()
            }
            rows.map { it.toModel() }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun syncActiveTicketStatuses(maxTickets: Int = 12) = withContext(Dispatchers.IO) {
        try {
            val tickets = getMyTickets()
            val active = tickets
                .filter { ticket ->
                    !isResolvedStatus(ticket.jiraStatus) &&
                        !(ticket.jiraStatus == "Email Sent" && ticket.ticketKey.startsWith("PENDING-"))
                }
                .take(maxTickets)
            for (ticket in active) {
                jiraService.syncTicketStatus(ticket.ticketKey)
                delay(250)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun JiraTicketRow.toModel() = JiraTicket(
        ticketKey = ticketKey,
        ticketUrl = ticketUrl,
        songType = songType,
        songNumber = songNumber,
        songTitle = songTitle,
        description = description,
        appVersion = appVersion,
        jiraStatus = jiraStatus,
        jiraStatusId = jiraStatusId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
