package com.agentOS.core.sandbox

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

object SandboxedDispatcher {
    /**
     * Create a limited-parallelism dispatcher for an agent.
     * Default: 2 threads max per agent (respects AgentScope.cpuLimitPercent indirectly)
     */
    fun forAgent(agentId: String, maxParallelism: Int = 2): CoroutineDispatcher {
        return Executors.newFixedThreadPool(maxParallelism) { r ->
            Thread(r, "agent-$agentId-${System.nanoTime()}")
        }.asCoroutineDispatcher()
    }
}
