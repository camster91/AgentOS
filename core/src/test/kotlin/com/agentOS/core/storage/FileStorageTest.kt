package com.agentOS.core.storage

import com.agentOS.api.AgentScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileStorageTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var storage: FileStorage

    private val scope = AgentScope(
        id = "test.file.agent",
        name = "File Test Agent",
        version = "1.0.0",
        author = "Test",
        description = "A test agent",
        storageQuotaBytes = 1024
    )

    @BeforeEach
    fun setUp() {
        storage = FileStorage(scope, tempDir)
    }

    @Test
    fun `write and read string`() = runBlocking {
        storage.writeString("greeting", "hello file")
        assertEquals("hello file", storage.readString("greeting"))
    }

    @Test
    fun `read missing key returns null`() = runBlocking {
        assertNull(storage.readString("nope"))
    }

    @Test
    fun `write and read bytes`() = runBlocking {
        val data = byteArrayOf(10, 20, 30)
        storage.writeBytes("bin", data)
        assertArrayEquals(data, storage.readBytes("bin"))
    }

    @Test
    fun `delete removes file`() = runBlocking {
        storage.writeString("key", "val")
        storage.delete("key")
        assertNull(storage.readString("key"))
    }

    @Test
    fun `clear removes all files`() = runBlocking {
        storage.writeString("a", "1")
        storage.writeString("b", "2")
        storage.clear()
        assertEquals(0, storage.listKeys().size)
    }

    @Test
    fun `listKeys returns stored keys`() = runBlocking {
        storage.writeString("x", "1")
        storage.writeString("y", "2")
        val keys = storage.listKeys().sorted()
        assertEquals(listOf("x", "y"), keys)
    }

    @Test
    fun `quota exceeded throws StorageQuotaExceededException`() = runBlocking {
        assertThrows<StorageQuotaExceededException> {
            runBlocking { storage.writeBytes("big", ByteArray(2000)) }
        }
    }

    @Test
    fun `key sanitization prevents path traversal`() = runBlocking {
        storage.writeString("../../etc/passwd", "safe")
        // The file should be stored inside tempDir, not escape it
        val keys = storage.listKeys()
        assertEquals(1, keys.size)
        // Sanitized key should not contain path separators
        assertFalse(keys[0].contains("/"))
        assertFalse(keys[0].contains(".."))
        // Verify we can read it back with the original key
        assertEquals("safe", storage.readString("../../etc/passwd"))
    }

    @Test
    fun `key with slashes is sanitized`() = runBlocking {
        storage.writeString("path/to/file", "content")
        assertEquals("content", storage.readString("path/to/file"))
        // File should be in root dir, not a subdirectory
        val files = tempDir.listFiles()!!
        assertEquals(1, files.size)
        assertEquals(tempDir, files[0].parentFile)
    }

    @Test
    fun `getSize returns total directory size`() = runBlocking {
        storage.writeString("a", "hello") // 5 bytes
        storage.writeString("b", "world") // 5 bytes
        assertEquals(10L, storage.getSize())
    }
}
