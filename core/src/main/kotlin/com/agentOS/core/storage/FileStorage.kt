package com.agentOS.core.storage

import com.agentOS.api.AgentScope
import com.agentOS.api.StorageAPI
import java.io.File

class FileStorage(private val scope: AgentScope, private val rootDir: File) : StorageAPI {

    init {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
    }

    private fun sanitizeKey(key: String): String {
        return key.replace("..", "_").replace("/", "_").replace("\\", "_")
    }

    private fun fileFor(key: String): File {
        return File(rootDir, sanitizeKey(key))
    }

    private fun directorySize(): Long {
        val files = rootDir.listFiles() ?: return 0L
        return files.sumOf { it.length() }
    }

    override suspend fun writeString(key: String, value: String) {
        writeBytes(key, value.toByteArray(Charsets.UTF_8))
    }

    override suspend fun readString(key: String): String? {
        val file = fileFor(key)
        if (!file.exists()) return null
        return file.readText(Charsets.UTF_8)
    }

    override suspend fun writeBytes(key: String, value: ByteArray) {
        val file = fileFor(key)
        val currentSize = directorySize()
        val existingSize = if (file.exists()) file.length() else 0L
        val newTotal = currentSize - existingSize + value.size
        if (newTotal > scope.storageQuotaBytes) {
            throw StorageQuotaExceededException(
                "Write would exceed storage quota: $newTotal > ${scope.storageQuotaBytes} bytes"
            )
        }
        file.writeBytes(value)
    }

    override suspend fun readBytes(key: String): ByteArray? {
        val file = fileFor(key)
        if (!file.exists()) return null
        return file.readBytes()
    }

    override suspend fun delete(key: String) {
        fileFor(key).delete()
    }

    override suspend fun listKeys(): List<String> {
        val files = rootDir.listFiles() ?: return emptyList()
        return files.map { it.name }
    }

    override suspend fun clear() {
        rootDir.listFiles()?.forEach { it.delete() }
    }

    override suspend fun getSize(): Long {
        return directorySize()
    }
}
