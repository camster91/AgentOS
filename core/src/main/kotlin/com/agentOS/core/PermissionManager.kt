package com.agentOS.core

import com.agentOS.api.AgentScope

class PermissionDeniedException(message: String) : RuntimeException(message)

class PermissionManager(private val scope: AgentScope) {

    companion object {
        val VALID_CAPABILITIES = setOf(
            "storage", "ui", "notifications", "calendar",
            "contacts", "messaging", "health", "cloud_sync"
        )
    }

    fun hasCapability(capability: String): Boolean {
        return scope.capabilities.contains(capability) ||
                scope.optionalCapabilities.contains(capability)
    }

    fun requireCapability(capability: String) {
        if (!hasCapability(capability)) {
            throw PermissionDeniedException(
                "Agent '${scope.id}' does not have capability '$capability'"
            )
        }
    }
}
