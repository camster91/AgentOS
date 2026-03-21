package com.agentOS.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class AgentEvent(
    val type: String,
    val agentId: String,
    val payload: Map<String, Any> = emptyMap()
)

object EventBus {

    private val _events = MutableSharedFlow<AgentEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AgentEvent> = _events.asSharedFlow()

    suspend fun publish(event: AgentEvent) {
        _events.emit(event)
    }

    fun subscribe(scope: CoroutineScope, handler: suspend (AgentEvent) -> Unit): Job {
        return scope.launch {
            events.collect { handler(it) }
        }
    }
}
