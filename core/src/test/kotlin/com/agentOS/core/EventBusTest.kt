package com.agentOS.core

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

class EventBusTest {

    @Test
    fun `publish and receive event`() = runTest {
        val received = CopyOnWriteArrayList<AgentEvent>()
        val job = EventBus.subscribe(this) { received.add(it) }

        // Give subscriber time to start collecting
        yield()

        val event = AgentEvent("test.event", "agent1", mapOf("key" to "value"))
        EventBus.publish(event)

        // Give time for event propagation
        yield()

        assertEquals(1, received.size)
        assertEquals("test.event", received[0].type)
        assertEquals("agent1", received[0].agentId)
        assertEquals("value", received[0].payload["key"])

        job.cancel()
    }

    @Test
    fun `multiple subscribers receive same event`() = runTest {
        val received1 = CopyOnWriteArrayList<AgentEvent>()
        val received2 = CopyOnWriteArrayList<AgentEvent>()

        val job1 = EventBus.subscribe(this) { received1.add(it) }
        val job2 = EventBus.subscribe(this) { received2.add(it) }

        yield()

        EventBus.publish(AgentEvent("multi", "agent2"))

        yield()

        assertEquals(1, received1.size)
        assertEquals(1, received2.size)
        assertEquals("multi", received1[0].type)
        assertEquals("multi", received2[0].type)

        job1.cancel()
        job2.cancel()
    }
}
