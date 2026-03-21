package com.agentOS.core.sandbox

import com.agentOS.api.AgentScope
import com.agentOS.core.storage.StorageQuotaExceededException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SandboxedStorageTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var storage: SandboxedStorage

    private val scope = AgentScope(
        id = "test.sandbox.agent",
        name = "Sandbox Test Agent",
        version = "1.0.0",
        author = "Test",
        description = "A test agent",
        storageQuotaBytes = 1024
    )

    @BeforeEach
    fun setUp() {
        storage = SandboxedStorage(scope, tempDir)
    }

    @Test
    fun `write and read within sandbox works`() = runBlocking {
        storage.writeString("mykey", "myvalue")
        assertEquals("myvalue", storage.readString("mykey"))
    }

    @Test
    fun `key with dotdot throws IllegalArgumentException`() = runBlocking {
        assertThrows<IllegalArgumentException> {
            runBlocking { storage.writeString("../../etc/passwd", "bad") }
        }
    }

    @Test
    fun `key starting with slash throws IllegalArgumentException`() = runBlocking {
        assertThrows<IllegalArgumentException> {
            runBlocking { storage.writeString("/etc/passwd", "bad") }
        }
    }

    @Test
    fun `quota exceeded throws StorageQuotaExceededException`() = runBlocking {
        assertThrows<StorageQuotaExceededException> {
            runBlocking { storage.writeBytes("big", ByteArray(2000)) }
        }
    }

    @Test
    fun `delete within sandbox works`() = runBlocking {
        storage.writeString("del", "value")
        storage.delete("del")
        assertNull(storage.readString("del"))
    }

    @Test
    fun `listKeys returns stored keys`() = runBlocking {
        storage.writeString("a", "1")
        storage.writeString("b", "2")
        val keys = storage.listKeys().sorted()
        assertEquals(listOf("a", "b"), keys)
    }

    @Test
    fun `getSize returns total storage size`() = runBlocking {
        storage.writeString("x", "hello") // 5 bytes
        storage.writeString("y", "world") // 5 bytes
        assertEquals(10L, storage.getSize())
    }
}
