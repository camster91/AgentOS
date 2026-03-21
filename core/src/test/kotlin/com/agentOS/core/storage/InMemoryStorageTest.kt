package com.agentOS.core.storage

import com.agentOS.api.AgentScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InMemoryStorageTest {

    private lateinit var storage: InMemoryStorage

    private val scope = AgentScope(
        id = "test.agent",
        name = "Test Agent",
        version = "1.0.0",
        author = "Test",
        description = "A test agent",
        storageQuotaBytes = 1024
    )

    @BeforeEach
    fun setUp() {
        storage = InMemoryStorage(scope)
    }

    @Test
    fun `write and read string`() = runBlocking {
        storage.writeString("key1", "hello world")
        assertEquals("hello world", storage.readString("key1"))
    }

    @Test
    fun `read missing key returns null`() = runBlocking {
        assertNull(storage.readString("missing"))
    }

    @Test
    fun `write and read bytes`() = runBlocking {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        storage.writeBytes("bin", data)
        assertArrayEquals(data, storage.readBytes("bin"))
    }

    @Test
    fun `read missing bytes returns null`() = runBlocking {
        assertNull(storage.readBytes("missing"))
    }

    @Test
    fun `delete removes key`() = runBlocking {
        storage.writeString("key1", "value")
        storage.delete("key1")
        assertNull(storage.readString("key1"))
    }

    @Test
    fun `clear removes all keys`() = runBlocking {
        storage.writeString("a", "1")
        storage.writeString("b", "2")
        storage.clear()
        assertEquals(0, storage.listKeys().size)
        assertEquals(0L, storage.getSize())
    }

    @Test
    fun `listKeys returns all stored keys`() = runBlocking {
        storage.writeString("x", "1")
        storage.writeString("y", "2")
        storage.writeString("z", "3")
        val keys = storage.listKeys().sorted()
        assertEquals(listOf("x", "y", "z"), keys)
    }

    @Test
    fun `getSize returns correct total bytes`() = runBlocking {
        storage.writeString("a", "hello") // 5 bytes
        storage.writeString("b", "world") // 5 bytes
        assertEquals(10L, storage.getSize())
    }

    @Test
    fun `quota exceeded throws StorageQuotaExceededException`() = runBlocking {
        val bigData = ByteArray(2000) // exceeds 1024 quota
        assertThrows<StorageQuotaExceededException> {
            runBlocking { storage.writeBytes("big", bigData) }
        }
    }

    @Test
    fun `overwrite existing key respects quota correctly`() = runBlocking {
        storage.writeBytes("key", ByteArray(500))
        // Overwrite with same size should work (500 replaced by 500, total still 500)
        storage.writeBytes("key", ByteArray(500))
        assertEquals(500L, storage.getSize())
    }

    @Test
    fun `overwrite existing key that would exceed quota throws`() = runBlocking {
        storage.writeBytes("a", ByteArray(500))
        storage.writeBytes("b", ByteArray(500))
        // Try to overwrite "a" with something that pushes total over 1024
        assertThrows<StorageQuotaExceededException> {
            runBlocking { storage.writeBytes("a", ByteArray(600)) }
        }
    }
}
