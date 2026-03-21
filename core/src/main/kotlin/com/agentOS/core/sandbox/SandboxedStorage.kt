package com.agentOS.core.sandbox

import com.agentOS.api.AgentScope
import com.agentOS.api.StorageAPI
import com.agentOS.core.storage.FileStorage
import java.io.File

class SandboxedStorage(scope: AgentScope, private val sandboxDir: File) : StorageAPI {
    private val inner = FileStorage(scope, sandboxDir)
    private val canonicalSandbox = sandboxDir.canonicalPath

    private fun validateKey(key: String) {
        require(!key.contains("..")) { "Key cannot contain '..' path traversal" }
        require(!key.startsWith("/")) { "Key cannot be absolute path" }
        require(!key.startsWith("\\")) { "Key cannot be absolute path" }
        val resolved = File(sandboxDir, key).canonicalPath
        require(resolved.startsWith(canonicalSandbox)) {
            "Key resolves outside sandbox directory"
        }
    }

    override suspend fun writeString(key: String, value: String) {
        validateKey(key)
        inner.writeString(key, value)
    }

    override suspend fun readString(key: String): String? {
        validateKey(key)
        return inner.readString(key)
    }

    override suspend fun writeBytes(key: String, value: ByteArray) {
        validateKey(key)
        inner.writeBytes(key, value)
    }

    override suspend fun readBytes(key: String): ByteArray? {
        validateKey(key)
        return inner.readBytes(key)
    }

    override suspend fun delete(key: String) {
        validateKey(key)
        inner.delete(key)
    }

    override suspend fun listKeys(): List<String> {
        return inner.listKeys()
    }

    override suspend fun clear() {
        inner.clear()
    }

    override suspend fun getSize(): Long {
        return inner.getSize()
    }
}
