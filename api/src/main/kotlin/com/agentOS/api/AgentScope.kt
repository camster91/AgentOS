package com.agentOS.api

/**
 * AgentScope — Immutable declaration of what an agent can do.
 *
 * This is the contract between the agent and the OS.
 * Agents cannot request more capabilities or resources at runtime.
 * This is what makes AgentOS fail-proof.
 */
data class AgentScope(
    /** Unique identifier for the agent (e.g., "com.example.budget") */
    val id: String,
    
    /** Human-readable agent name */
    val name: String,
    
    /** Version (e.g., "1.0.0") */
    val version: String,
    
    /** Developer name */
    val author: String,
    
    /** Short description of what the agent does */
    val description: String,
    
    /** Icon (optional, base64-encoded PNG or URL) */
    val icon: String? = null,
    
    /**
     * Required capabilities.
     * Agent cannot function without these.
     * Examples: "storage", "ui", "notifications"
     */
    val capabilities: Set<String> = emptySet(),
    
    /**
     * Optional capabilities.
     * User grants these at install time.
     * Examples: "contacts", "calendar", "location"
     */
    val optionalCapabilities: Set<String> = emptySet(),
    
    /**
     * Maximum storage this agent can use (bytes).
     * Default: 100MB
     * Hard limit enforced by OS.
     */
    val storageQuotaBytes: Long = 100_000_000,
    
    /**
     * Maximum CPU percentage this agent can use.
     * Default: 5%
     * If agent exceeds this, OS throttles or kills it.
     */
    val cpuLimitPercent: Int = 5,
    
    /**
     * Maximum RAM this agent can use (bytes).
     * Default: 200MB
     * If agent exceeds this, OS kills it to free memory.
     */
    val ramLimitBytes: Long = 200_000_000,
    
    /**
     * What the agent says when asked to do something out of scope.
     */
    val outOfScopeResponse: String = "I can't help with that. Let me tell you what I can do..."
) {
    init {
        require(id.isNotBlank()) { "Agent ID cannot be empty" }
        require(name.isNotBlank()) { "Agent name cannot be empty" }
        require(version.isNotBlank()) { "Agent version cannot be empty" }
        require(author.isNotBlank()) { "Agent author cannot be empty" }
        require(storageQuotaBytes > 0) { "Storage quota must be positive" }
        require(cpuLimitPercent in 1..100) { "CPU limit must be 1-100%" }
        require(ramLimitBytes > 0) { "RAM limit must be positive" }
    }

    /**
     * Check if agent has a required capability.
     */
    fun hasCapability(cap: String): Boolean = capabilities.contains(cap)

    /**
     * Check if agent has an optional capability (user-granted).
     */
    fun hasOptionalCapability(cap: String): Boolean = optionalCapabilities.contains(cap)

    /**
     * All capabilities this agent requests (required + optional).
     */
    fun allRequestedCapabilities(): Set<String> = capabilities + optionalCapabilities

    override fun toString(): String =
        "$name v$version by $author — ${capabilities.size} required, ${optionalCapabilities.size} optional capabilities"
}
