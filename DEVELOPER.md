# AgentOS Developer Guide

## Module Structure

```
AgentOS/
├── core/                    # JVM agent runtime
│   ├── AgentOS.kt           # Main orchestrator
│   ├── AgentRegistry.kt     # Agent registration + routing
│   ├── ConversationMemory.kt # Per-agent history persistence
│   ├── agents/
│   │   ├── NotesAgent.kt
│   │   ├── CalendarAgent.kt
│   │   ├── TasksAgent.kt
│   │   └── WeatherAgent.kt
│   └── storage/
│       ├── FileStorage.kt   # Persistent key-value store
│       └── InMemoryStorage.kt
├── agents/ai-core/          # Gemini AI client
│   └── GeminiClient.kt      # HTTP client + ChatTurn
├── android/                 # Android shell
│   ├── MainActivity.kt      # Chat UI entry point
│   └── AgentOSApplication.kt
├── sync/                    # Cloud sync backend
│   ├── SyncServer.kt        # Ktor HTTP server
│   ├── SyncClient.kt        # OkHttp client
│   └── DeviceId.kt
└── api/                     # Shared interfaces
```

## Adding a New Agent

1. Create `YourAgent.kt` in `core/src/main/kotlin/com/agentOS/agents/`
2. Implement the `AgentAPI` interface
3. Register in `AgentRegistry.kt`
4. Add tests in `core/src/test/`

## Running Tests

```bash
./gradlew test
```

## Build Variants

- Debug APK: `./gradlew :android:assembleDebug`
- Release APK: `./gradlew :android:assembleRelease` (requires keystore)
- Sync server: `./gradlew :sync:run`
