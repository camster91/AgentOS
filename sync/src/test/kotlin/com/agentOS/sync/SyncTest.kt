package com.agentOS.sync

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SyncTest {

    @Test
    fun `SyncEntry data class holds correct values`() {
        val entry = SyncEntry(
            userId = "user1",
            agentId = "notes",
            key = "note-1",
            value = "Hello world",
            timestamp = 1000L
        )
        assertEquals("user1", entry.userId)
        assertEquals("notes", entry.agentId)
        assertEquals("note-1", entry.key)
        assertEquals("Hello world", entry.value)
        assertEquals(1000L, entry.timestamp)
    }

    @Test
    fun `SyncEntry copy works correctly`() {
        val entry = SyncEntry("u1", "a1", "k1", "v1", 100L)
        val updated = entry.copy(value = "v2", timestamp = 200L)
        assertEquals("v2", updated.value)
        assertEquals(200L, updated.timestamp)
        assertEquals("u1", updated.userId)
    }

    @Test
    fun `SyncClient push returns false on connection error`() {
        val client = SyncClient("http://localhost:19999")
        val result = client.push("user1", "notes", "key1", "value1")
        assertFalse(result)
    }

    @Test
    fun `SyncClient pull returns empty list on connection error`() {
        val client = SyncClient("http://localhost:19999")
        val result = client.pull("user1", "notes")
        assertEquals(emptyList(), result)
    }
}
