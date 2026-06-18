package com.reyzie.hymns.data

import android.content.Context
import android.util.Log

data class ResolvedTicketAckItem(
    val ticketKey: String,
    val songType: String,
    val songNumber: Int,
    val songTitle: String,
    val jiraStatus: String
)

class TicketAcknowledgementService(context: Context) {
    private val prefs = context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
    private val ticketsRepository = TicketsRepository(context)
    private val jiraService = JiraService()

    companion object {
        private const val ACK_STORAGE_KEY = "acknowledged_resolved_ticket_keys_v1"
        private const val MAX_STORED_KEYS = 500
    }

    suspend fun getUnacknowledgedResolvedTickets(syncFirst: Boolean = true): List<ResolvedTicketAckItem> {
        return try {
            if (syncFirst) {
                ticketsRepository.syncActiveTicketStatuses(maxTickets = 12)
            }
            val acknowledged = loadAcknowledgedKeys()
            ticketsRepository.getMyTickets()
                .filter { ticket ->
                    TicketsRepository.isResolvedStatus(ticket.jiraStatus) &&
                        !ticket.ticketKey.startsWith("PENDING-") &&
                        ticket.ticketKey !in acknowledged
                }
                .map { ticket ->
                    ResolvedTicketAckItem(
                        ticketKey = ticket.ticketKey,
                        songType = ticket.songType,
                        songNumber = ticket.songNumber,
                        songTitle = ticket.songTitle,
                        jiraStatus = ticket.jiraStatus
                    )
                }
        } catch (e: Exception) {
            Log.w("TicketAckService", "Failed to load resolved tickets", e)
            emptyList()
        }
    }

    fun markAcknowledged(ticketKeys: Iterable<String>) {
        val keys = ticketKeys.toSet()
        if (keys.isEmpty()) return
        val current = prefs.getStringSet(ACK_STORAGE_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.addAll(keys)
        val trimmed = if (current.size > MAX_STORED_KEYS) {
            current.toList().takeLast(MAX_STORED_KEYS).toSet()
        } else {
            current
        }
        prefs.edit().putStringSet(ACK_STORAGE_KEY, trimmed).apply()
    }

    private fun loadAcknowledgedKeys(): Set<String> =
        prefs.getStringSet(ACK_STORAGE_KEY, emptySet()) ?: emptySet()
}
