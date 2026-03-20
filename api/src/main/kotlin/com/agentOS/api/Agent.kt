package com.agentOS.api

/**
 * Agent — Base class for all agents in AgentOS.
 *
 * Every agent extends this class and declares its scope (capabilities, limits).
 * Agents communicate with users through the chat protocol.
 */
abstract class Agent {
    /**
     * Immutable scope declaration.
     * Defines what the agent can do, what resources it has, and how it behaves.
     * Cannot be modified at runtime.
     */
    abstract val scope: AgentScope

    /**
     * The API surface available to this agent.
     * Enforced at runtime — agents can only call APIs they have capabilities for.
     */
    abstract val api: AgentAPI

    /**
     * Main chat handler.
     * User sends a message, agent responds.
     * If user request is out of scope, agent gracefully declines.
     */
    abstract suspend fun onChat(message: ChatMessage): ChatMessage

    /**
     * Called when the agent is installed and configured.
     * Agent can set up initial state, validate capabilities, etc.
     */
    open suspend fun onInstall(grantedCapabilities: Set<String>) {
        println("Agent installed: ${scope.name}")
    }

    /**
     * Called when the agent is about to be uninstalled.
     * Agent should clean up resources, save critical state, etc.
     * CANNOT prevent uninstall — user's choice is final.
     */
    open suspend fun onUninstall() {
        println("Agent uninstalling: ${scope.name}")
    }

    /**
     * Called periodically by the OS (e.g., every hour or on idle).
     * Agent can perform background tasks (sync, cleanup, etc.)
     * Must respect CPU limits.
     */
    open suspend fun onBackground() {}

    /**
     * Get agent metadata for marketplace/UI display.
     */
    fun getMetadata(): AgentMetadata = AgentMetadata(
        id = scope.id,
        name = scope.name,
        version = scope.version,
        author = scope.author,
        description = scope.description,
        icon = scope.icon,
        capabilities = scope.capabilities,
        optionalCapabilities = scope.optionalCapabilities,
        rating = 0.0f, // Populated from marketplace
        downloads = 0 // Populated from marketplace
    )
}

/**
 * ChatMessage — Standard message format for agent communication.
 */
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
) {
    init {
        require(role in setOf("user", "assistant")) { "Role must be 'user' or 'assistant'" }
        require(text.isNotBlank()) { "Message text cannot be empty" }
    }
}

/**
 * AgentMetadata — Public metadata for marketplace display.
 */
data class AgentMetadata(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val icon: ByteArray? = null,
    val capabilities: Set<String>,
    val optionalCapabilities: Set<String>,
    val rating: Float = 0.0f,
    val downloads: Long = 0
)
