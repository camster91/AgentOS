package com.agentOS.api

/**
 * AgentAPI — The standardized interface agents use to interact with the OS.
 *
 * Each capability is access-controlled.
 * Agents can only call APIs they have capabilities for.
 */
interface AgentAPI {
    /** Storage API — read/write agent's sandbox */
    val storage: StorageAPI

    /** UI API — display messages, show options, request input */
    val ui: UIAPI

    /** Notifications API — send alerts to user */
    val notifications: NotificationsAPI

    /** Calendar API (capability-gated) */
    val calendar: CalendarAPI?

    /** Contacts API (capability-gated) */
    val contacts: ContactsAPI?

    /** Messaging API (capability-gated) */
    val messaging: MessagingAPI?

    /** Health/Fitness API (capability-gated) */
    val health: HealthAPI?

    /** Cloud Sync API (capability-gated) */
    val cloudSync: CloudSyncAPI?
}

/**
 * StorageAPI — Persistent storage for agent data.
 * Each agent has an isolated 100MB sandbox.
 */
interface StorageAPI {
    suspend fun writeString(key: String, value: String)
    suspend fun readString(key: String): String?
    suspend fun writeBytes(key: String, value: ByteArray)
    suspend fun readBytes(key: String): ByteArray?
    suspend fun delete(key: String)
    suspend fun listKeys(): List<String>
    suspend fun clear()
    suspend fun getSize(): Long // Current usage in bytes
}

/**
 * UIAPI — Display interface to the user.
 */
interface UIAPI {
    suspend fun showMessage(message: String)
    suspend fun showOptions(
        message: String,
        options: List<String>,
        onSelected: suspend (String) -> Unit
    )
    suspend fun requestInput(
        prompt: String,
        type: String = "text" // "text", "number", "date", "time"
    ): String?
    suspend fun showError(message: String)
    suspend fun showSuccess(message: String)
}

/**
 * NotificationsAPI — Alert the user.
 */
interface NotificationsAPI {
    suspend fun send(
        title: String,
        message: String,
        priority: String = "normal" // "low", "normal", "high"
    )
}

/**
 * CalendarAPI — Access calendar events (capability-gated).
 */
interface CalendarAPI {
    data class Event(
        val id: String,
        val title: String,
        val startTime: Long,
        val duration: Long,
        val location: String? = null,
        val attendees: List<String> = emptyList()
    )

    suspend fun createEvent(
        title: String,
        startTime: Long,
        duration: Long,
        location: String? = null
    ): Event

    suspend fun getEvents(startDate: Long, endDate: Long): List<Event>
    suspend fun updateEvent(event: Event)
    suspend fun deleteEvent(eventId: String)
}

/**
 * ContactsAPI — Access user's contacts (capability-gated).
 */
interface ContactsAPI {
    data class Contact(
        val id: String,
        val name: String,
        val email: String? = null,
        val phone: String? = null,
        val avatar: ByteArray? = null
    )

    suspend fun listAll(): List<Contact>
    suspend fun find(query: String): List<Contact>
    suspend fun findByEmail(email: String): Contact?
    suspend fun findByPhone(phone: String): Contact?
}

/**
 * MessagingAPI — Send messages (SMS, RCS, etc.) (capability-gated).
 */
interface MessagingAPI {
    data class Message(
        val id: String,
        val recipientId: String,
        val text: String,
        val timestamp: Long,
        val isRead: Boolean
    )

    suspend fun sendMessage(recipientId: String, text: String): Message
    suspend fun getConversation(contactId: String, limit: Int = 100): List<Message>
    suspend fun markAsRead(messageId: String)
}

/**
 * HealthAPI — Access health/fitness data (capability-gated).
 */
interface HealthAPI {
    data class HealthRecord(
        val type: String, // "steps", "heart_rate", "sleep", "exercise"
        val value: Double,
        val unit: String,
        val timestamp: Long
    )

    suspend fun recordSteps(steps: Int, timestamp: Long)
    suspend fun recordHeartRate(bpm: Int, timestamp: Long)
    suspend fun recordSleep(duration: Long, timestamp: Long)
    suspend fun getRecords(type: String, startDate: Long, endDate: Long): List<HealthRecord>
}

/**
 * CloudSyncAPI — Cloud backup and multi-device sync (capability-gated).
 */
interface CloudSyncAPI {
    suspend fun backup(): Boolean
    suspend fun restore(): Boolean
    suspend fun getLastSyncTime(): Long?
    suspend fun isEnabled(): Boolean
}
