package com.agentOS.core

import com.agentOS.api.Agent
import com.agentOS.api.AgentMetadata
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class AgentRegistry {

    private val agents = ConcurrentHashMap<String, Agent>()

    fun register(agent: Agent): Boolean {
        return agents.putIfAbsent(agent.scope.id, agent) == null
    }

    fun unregister(agentId: String): Boolean {
        val agent = agents.remove(agentId) ?: return false
        runBlocking { agent.onUninstall() }
        return true
    }

    fun getAgent(agentId: String): Agent? {
        return agents[agentId]
    }

    fun listAgents(): List<AgentMetadata> {
        return agents.values.map { it.getMetadata() }
    }

    fun getCount(): Int {
        return agents.size
    }
}
