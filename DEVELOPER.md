# AgentOS Developer Guide

Welcome to AgentOS development. This guide covers building custom agents, understanding the core architecture, and contributing to the project.

## Quick Start: Building Your First Agent

Every agent is a Kotlin class that extends `Agent` and implements the chat protocol.

```kotlin
import agentOS.api.Agent
import agentOS.api.AgentScope
import agentOS.api.ChatMessage

class MyCustomAgent : Agent {
    override val scope = AgentScope(
        name = "My Custom Agent",
        capabilities = setOf("storage", "ui", "notifications"),
        storageQuota = 50_000_000, // 50MB
        cpuLimit = 5, // 5%
        description = "Does something useful"
    )

    override suspend fun onChat(message: ChatMessage): ChatMessage {
        val userInput = message.text
        val response = generateResponse(userInput)
        return ChatMessage(
            role = "assistant",
            text = response,
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun generateResponse(input: String): String {
        // Call Gemini API or use local logic
        return "You said: $input"
    }
}
```

## Core Concepts

### Agent Scope
Every agent declares its **scope** — what it can do and what resources it needs.

```kotlin
val scope = AgentScope(
    name = "Budget Agent",
    description = "Track spending and create budgets",
    capabilities = setOf(
        "storage",           // Can read/write its own sandbox
        "notifications",     // Can send notifications
        "contacts"          // Optional: needs user permission
    ),
    storageQuota = 100_000_000,  // 100MB max
    cpuLimit = 10,               // Max 10% of system CPU
    ramLimit = 200_000_000,      // 200MB RAM
    outOfScopeResponse = "I can only help with budgeting. For investment advice, try the Finance Agent."
)
```

Scopes are **immutable** — agents cannot request additional capabilities or resources at runtime.

### Chat Protocol

Agents communicate with users and other agents through the chat protocol:

```kotlin
data class ChatMessage(
    val role: String,           // "user" or "assistant"
    val text: String,           // The message content
    val timestamp: Long,        // Unix timestamp
    val metadata: Map<String, Any> = emptyMap()
)
```

When a user asks something outside an agent's scope, the agent gracefully declines:

```
User: "Can you invest my money in the stock market?"
Agent: "I can't execute trades, but I can help you track your portfolio and set financial goals. Would you like to do either of those?"
```

### Agent Lifecycle

1. **Install** — User installs agent from marketplace
2. **Grant Permissions** — User approves capabilities
3. **Configure** — User sets preferences (via chat)
4. **Run** — Agent executes, monitored by OS
5. **Update** — Developer releases new version
6. **Uninstall** — User removes agent, data wiped

### Agent Manifest

Each agent includes a manifest declaring its identity and scope:

```json
{
  "id": "com.example.budget",
  "name": "Budget Agent",
  "version": "1.0.0",
  "author": "Example Dev",
  "description": "Track spending and create budgets",
  "icon": "icon.png",
  "capabilities": ["storage", "notifications"],
  "optionalCapabilities": ["contacts"],
  "resourceLimits": {
    "storageQuota": 100000000,
    "cpuLimit": 10,
    "ramLimit": 200000000
  },
  "minAndroidVersion": 14,
  "signature": "ed25519-signature-here"
}
```

## Agent API Reference

### Storage API

```kotlin
// Access agent's isolated storage (100MB sandbox)
val storage = agent.api.storage

// Write data
storage.writeString("key", "value")
storage.writeBytes("key", byteArray)

// Read data
val value = storage.readString("key")
val bytes = storage.readBytes("key")

// Query
val keys = storage.listKeys()
storage.delete("key")
```

### UI API

```kotlin
val ui = agent.api.ui

// Display message
ui.showMessage("Hello, user!")

// Show options
ui.showOptions(
    message = "Which view would you prefer?",
    options = listOf("Weekly", "Monthly", "Yearly")
) { selected ->
    // User selected an option
}

// Show input dialog
ui.requestInput(
    prompt = "Enter amount:",
    type = "number"
) { value ->
    // User entered value
}
```

### Notifications API

```kotlin
val notifications = agent.api.notifications

notifications.send(
    title = "Budget Alert",
    message = "You've exceeded your spending limit",
    priority = "high"
)
```

### Calendar API (Capability-gated)

```kotlin
// Only available if user granted "calendar" capability

val calendar = agent.api.calendar

calendar.createEvent(
    title = "Meeting",
    startTime = System.currentTimeMillis(),
    duration = 3600000 // 1 hour
)

val events = calendar.getEvents(startDate, endDate)
```

### Contacts API (Capability-gated)

```kotlin
// Only available if user granted "contacts" capability

val contacts = agent.api.contacts

val allContacts = contacts.listAll()
val specific = contacts.find("email@example.com")
```

### Cloud Sync API (Capability-gated)

```kotlin
// Only available if user has sync enabled

val sync = agent.api.cloudSync

sync.backup() // Upload to cloud
sync.restore() // Download from cloud
```

## Building & Testing

### Local Testing

```bash
# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Run instrumented tests (Android device/emulator)
./gradlew connectedAndroidTest
```

### Sandbox Testing

Verify your agent respects scope boundaries:

```kotlin
@Test
fun testAgentCantEscapeSandbox() {
    val agent = MyCustomAgent()
    
    // This should fail gracefully, not crash
    assertThrows<OutOfScopeException> {
        agent.api.contacts.listAll() // Not in declared capabilities
    }
}

@Test
fun testAgentRespectsCPULimit() {
    val agent = MyCustomAgent()
    
    // Monitor CPU usage during heavy computation
    val cpuUsage = measureCPU {
        agent.onChat(ChatMessage("user", "Do something expensive"))
    }
    
    assertTrue(cpuUsage < 10) // Within 10% limit
}
```

## Debugging

### Logs

```bash
# View agent logs in real-time
./gradlew logcat | grep AgentOS

# Filter by agent
./gradlew logcat | grep "MyCustomAgent"
```

### Crash Reports

If an agent crashes, it's automatically caught and reported to the user:

```
Agent crashed: MyCustomAgent v1.0.0
Error: NullPointerException in onChat()
Timestamp: 2026-03-20T14:30:00Z
Stack trace: ...
```

Users can choose to disable the agent or report the issue.

## Publishing Your Agent

1. **Test thoroughly** — Verify all scope boundaries
2. **Sign your agent** — Create Ed25519 signature
3. **Create manifest** — Fill in metadata and capabilities
4. **Submit to marketplace** — AgentOS team reviews for safety
5. **Set pricing** — Free, freemium, or premium tiers
6. **Monitor ratings** — Users rate your agent, iterate

## Best Practices

### Do
- ✅ Declare all capabilities your agent needs
- ✅ Handle out-of-scope requests gracefully
- ✅ Cache API responses to minimize network calls
- ✅ Test extensively before publishing
- ✅ Provide clear, conversational responses
- ✅ Update regularly with bug fixes and features

### Don't
- ❌ Request capabilities you don't use
- ❌ Try to access restricted APIs (will fail)
- ❌ Use infinite loops or blocking operations
- ❌ Modify OS core or other agents
- ❌ Store data outside your sandbox
- ❌ Collect user data beyond your stated scope

## Architecture Overview

```
┌─────────────────────────────────────────┐
│          AgentOS Core (Immutable)       │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │    Permission & Sandbox Manager  │  │
│  └──────────────────────────────────┘  │
│           ↓         ↓         ↓         │
│  ┌────────────────────────────────────┐ │
│  │    Agent Runtime (WebAssembly)     │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
        ↓              ↓              ↓
   ┌────────┐    ┌────────┐    ┌────────┐
   │ Email  │    │ Calendar│   │ Task   │
   │ Agent  │    │ Agent   │   │ Agent  │
   └────────┘    └────────┘    └────────┘
```

Each agent runs in its own sandbox, isolated from others. The OS core enforces all safety guarantees.

## Need Help?

- **Documentation**: See `/docs` in the repo
- **GitHub Issues**: Report bugs or request features
- **Discussions**: Ask questions in GitHub Discussions
- **Discord**: Join the community server (link in README)

---

**Happy building! The future of personal computing is yours to shape.**
