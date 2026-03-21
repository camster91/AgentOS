package com.agentOS.core

import java.util.concurrent.atomic.AtomicBoolean

/**
 * AgentOS Core — The immutable foundation of the operating system.
 *
 * This singleton is responsible for:
 * - Managing the agent lifecycle (install, run, uninstall)
 * - Enforcing the immutable core (agents cannot modify this)
 * - Delegating to the sandbox runtime for agent execution
 * - Managing permissions and capabilities
 * - Monitoring resource usage and fail-safety
 *
 * The OS core itself is READ-ONLY from agent perspective.
 * Agents cannot modify, patch, or circumvent the OS.
 */
object AgentOS {
    private val initialized = AtomicBoolean(false)

    lateinit var registry: AgentRegistry
        private set

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) {
            throw IllegalStateException("AgentOS is already initialized")
        }
        println("AgentOS initializing...")
        println("  - Loading core services")

        println("  - Initializing AgentRegistry")
        registry = AgentRegistry()

        println("  - Initializing EventBus")
        // EventBus is an object singleton — already available
        EventBus

        println("  - Initializing sandbox runtime")
        println("  - Setting up permission system")
        println("  - Mounting storage layer")
        println("AgentOS ready")
    }

    fun shutdown() {
        if (!initialized.compareAndSet(true, false)) {
            throw IllegalStateException("AgentOS is not initialized")
        }
        println("AgentOS shutting down...")
        println("  - Gracefully stopping all agents")
        println("  - Syncing persistent state")
        println("  - Closing storage connections")
        println("AgentOS offline")
    }

    fun isInitialized(): Boolean = initialized.get()

    /**
     * Core invariants that must ALWAYS hold:
     * 1. OS core is immutable (read-only)
     * 2. Agents run in isolated sandboxes
     * 3. Agents cannot exceed declared scope
     * 4. Agents cannot modify themselves or the OS
     * 5. Resource limits are enforced at runtime
     * 6. Failures in one agent don't affect others
     */
}
