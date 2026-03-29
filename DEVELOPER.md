# AgentOS Developer Guide

See [CLAUDE.md](CLAUDE.md) for the AI-assisted development guide (conventions, adding agents, testing patterns).

---

## Module Structure

```
AgentOS/
├── api/                     # Shared interfaces (Agent, AgentScope, StorageAPI, ChatMessage)
├── agents/ai-core/          # GeminiClient — Gemini 2.0 Flash via OkHttp
├── core/                    # JVM runtime
│   └── src/main/kotlin/com/agentOS/
│       ├── core/            # AgentOS, AgentRegistry, EventBus, PermissionManager
│       ├── agents/          # NotesAgent, CalendarAgent, TasksAgent, WeatherAgent,
│       │                    # EmailAgent, MessagingAgent, FinanceAgent
│       └── core/storage/    # FileStorage, InMemoryStorage
├── sync/                    # Ktor HTTP sync server + OkHttp client
└── android/
    ├── src/main/java/       # AgentOSApplication (Application subclass)
    └── src/main/kotlin/     # ChatViewModel, Navigation, OnboardingScreen, ChatScreen,
                             # SettingsScreen, AgentSelectorBar, AgentOSTheme
```

---

## Build Commands

```bash
# JVM modules (works without Android Studio)
gradle :core:build --no-daemon
gradle :sync:build --no-daemon
gradle :agents:ai-core:build --no-daemon

# Run the CLI
export GEMINI_API_KEY=your_key
gradle :core:run --no-daemon

# Run tests
gradle :core:test :sync:test --no-daemon

# Android (requires Google Maven / Android Studio)
gradle :android:assembleDebug --no-daemon
gradle :android:assembleRelease --no-daemon   # requires local.properties

# Sync server
gradle :sync:run --no-daemon
```

> Android builds fail in sandboxed environments (no Google Maven access). CI handles them.

---

## Adding a New Agent

1. **Create** `core/src/main/kotlin/com/agentOS/agents/MyAgent.kt` — extend `Agent()`, implement `onChat()`
2. **Register** in `core/src/main/kotlin/com/agentOS/core/Main.kt` — add scope, instantiate, register, add to `routeMessage()`
3. **Register** in `android/src/main/java/com/agentOS/android/AgentOSApplication.kt` — add to `agents` map
4. **Add display name** to `agentList` in `ChatViewModel.kt`
5. **Add description** to `allAgents` in `SettingsScreen.kt`
6. **Add example hints** to `agentHints` map in `ChatScreen.kt`
7. **Write tests** in `core/src/test/kotlin/com/agentOS/agents/MyAgentTest.kt`

Full template in [CLAUDE.md](CLAUDE.md#adding-a-new-agent).

---

## Testing

All tests use JUnit 5 + `kotlin("test-junit5")`.

```bash
gradle :core:test :sync:test --no-daemon
```

- Test files mirror source package structure in `src/test/kotlin/`
- Use `GeminiClient(apiKey = "test-key")` to trigger `"AI unavailable..."` fallback
- `InMemoryStorage` is used in all agent tests for isolation

Required in every module `build.gradle.kts` with tests:
```kotlin
testImplementation(kotlin("test-junit5"))
testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
testRuntimeOnly("org.junit.platform:junit-platform-launcher")
tasks.test { useJUnitPlatform() }
```

---

## Release Signing

Copy `local.properties.example` → `local.properties`:
```properties
KEYSTORE_PATH=android/agentOS-release.jks
KEYSTORE_PASSWORD=yourpassword
KEY_ALIAS=youralias
KEY_PASSWORD=yourkeypassword
```

`local.properties` is gitignored. CI injects signing credentials via repository secrets:
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

---

## Agent Storage Conventions

| Agent | Key Prefix | Format |
|-------|-----------|--------|
| Notes | `note-` | `id\|title\|createdAt\|linkedIds\|content` |
| Calendar | `event-` | `id\|title\|startTime\|endTime\|location` |
| Tasks | `task_` | `id\|title\|priority\|dueDate\|completed\|createdAt` |
| Email | `email_` | `id\|from\|to\|subject\|timestamp\|isRead\|folder\|body` |
| Messaging | `msg_` | `id\|contact\|timestamp\|isSent\|body` |
| Finance | `txn_`, `budget_` | `id\|description\|amount\|category\|timestamp` |

Serialization uses pipe-delimited strings — no external JSON library, simple and testable.
Keys are sanitized: `..`, `/`, `\` → `_`.

---

## Architecture Decisions

- **Immutable core**: `AgentOS` is a sealed singleton; agents can't modify the OS
- **Fail-safe AI**: All `gemini.chat()` calls are wrapped — returns `"AI unavailable..."` on any error
- **Per-agent storage isolation**: Each agent gets its own `FileStorage` directory
- **No Gson in agents**: Pipe-delimited serialization avoids reflection overhead and proguard issues
- **InMemoryStorage in tests**: Gives each test a clean slate; no file I/O needed
