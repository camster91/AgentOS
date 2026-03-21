package com.agentOS.core.storage

import com.agentOS.api.AgentScope
import com.agentOS.api.StorageAPI
import java.util.concurrent.ConcurrentHashMap

class InMemoryStorage(private val scope: AgentScope) : StorageAPI {

    private val store = ConcurrentHashMap<String, ByteArray>()

    override suspend fun writeString(key: String, value: String) {
        writeBytes(key, value.toByteArray(Charsets.UTF_8))
    }

    override suspend fun readString(key: String): String? {
        return store[key]?.toString(Charsets.UTF_8)
    }

    override suspend fun writeBytes(key: String, value: ByteArray) {
        val currentSize = getSize()
        val existingSize = store[key]?.size?.toLong() ?: 0L
        val newTotal = currentSize - existingSize + value.size
        if (newTotal > scope.storageQuotaBytes) {
            throw StorageQuotaExceededException(
                "Write would exceed storage quota: $newTotal > ${scope.storageQuotaBytes} bytes"
            )
        }
        store[key] = value
    }

    override suspend fun readBytes(key: String): ByteArray? {
        return store[key]
    }

    override suspend fun delete(key: String) {
        store.remove(key)
    }

    override suspend fun listKeys(): List<String> {
        return store.keys().toList()
    }

    override suspend fun clear() {
        store.clear()
    }

    override suspend fun getSize(): Long {
        return store.values.sumOf { it.size.toLong() }
    }
}
