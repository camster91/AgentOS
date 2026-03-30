# CLAUDE.md — AgentOS

AI assistant guide for developing AgentOS. Read this before making changes.

---

## Project Overview

AgentOS is a modular AI agent operating system. It replaces traditional apps with conversational agents powered by Google Gemini 2.0 Flash. There are two runtimes:
- **JVM CLI** (`core` module) — runs on any JDK 17+ machine
- **Android app** (`android` module) — Jetpack Compose chat UI

**Current version:** v0.3.0
**Development branch:** `claude/start-new-app-KOmLu`
**Always push to this branch. Never push to `main` directly.**

---

## Module Structure

```
AgentOS/
├── api/                     # Shared interfaces (Agent, AgentScope, StorageAPI, ChatMessage)
├── agents/ai-core/          # GeminiClient (Gemini 2.0 Flash via OkHttp REST)
├── core/                    # JVM runtime: registry, storage, sandbox, CLI (Main.kt)
│   └── src/main/kotlin/com/agentOS/
│       ├── core/            # AgentOS singleton, AgentRegistry, EventBus, PermissionManager
│       ├── agents/          # NotesAgent, CalendarAgent, TasksAgent, WeatherAgent,
│       │                    # EmailAgent, MessagingAgent, FinanceAgent
│       └── core/storage/    # FileStorage, InMemoryStorage
├── sync/                    # Ktor HTTP sync server + OkHttp client
└── android/                 # Jetpack Compose Android app
    ├── src/main/java/       # AgentOSApplication (Java package for Application subclass)
    └── src/main/kotlin/     # ChatViewModel, Navigation, all UI screens
```

---

## Build Commands

```bash
# Build everything except Android (Android plugin unavailable in sandbox)
gradle :core:build --no-daemon
gradle :sync:build --no-daemon
gradle :agents:ai-core:build --no-daemon

# Run tests (JVM modules only)
gradle :core:test :sync:test --no-daemon

# Run the CLI
gradle :core:run --no-daemon
# or with API key:
GEMINI_API_KEY=your_key gradle :core:run --no-daemon
```

> **Note:** Android builds (`gradle :android:build`) require the Google Maven repository and fail in sandboxed environments. CI handles Android builds.

---

## Adding a New Agent

1. Create `core/src/main/kotlin/com/agentOS/agents/MyAgent.kt`:

```kotlin
class MyAgent(private val storage: StorageAPI, private val gemini: GeminiClient) : Agent() {
    override val scope = AgentScope(
        id = "com.agentOS.myagent",
        name = "My Agent",
        version = "0.1.0",
        author = "Cameron",
        description = "What this agent does.",
        capabilities = setOf("storage", "ui")
    )
    override val api: AgentAPI = NoOpAgentAPI(storage)

    override suspend fun onChat(message: ChatMessage): ChatMessage {
        val text = message.text.trim()
        val response = when {
            text.lowercase() == "my command" -> handleCommand()
            else -> aiChat(text)
        }
        return ChatMessage("assistant", response)
    }

    private fun aiChat(userMessage: String) = gemini.chat(
        systemPrompt = "You are the My Agent for AgentOS...",
        userMessage = userMessage
    )
}
```

2. Register in `core/src/main/kotlin/com/agentOS/core/Main.kt` — add scope, instantiate, register, add to `routeMessage`.

3. Register in `android/src/main/java/com/agentOS/android/AgentOSApplication.kt` — add to the `agents` map.

4. Add display name to `agentList` in `android/src/main/kotlin/com/agentOS/android/ChatViewModel.kt`.

5. Add to `allAgents` list in `SettingsScreen.kt`.

6. Write tests in `core/src/test/kotlin/com/agentOS/agents/MyAgentTest.kt` (follow `NotesAgentTest` pattern).

---

## Key Conventions

### Agent Pattern
- Every agent extends `Agent()` from `:api`
- `onChat()` is the single entry point — returns a `ChatMessage`
- Pattern: keyword dispatch → `aiChat()` fallback
- `aiChat()` calls `gemini.chat(systemPrompt, userMessage)` — never throws (returns "AI unavailable" on error)
- Storage keys use agent-specific prefixes: `note_`, `task_`, `email_`, `msg_`, `txn_`, `budget_`
- Serialization is pipe-delimited strings (no Gson for agent data — keeps the format simple and testable)

### Storage
- **JVM:** `InMemoryStorage` for CLI (session only), `FileStorage` for persistence
- **Android:** `FileStorage` backed by `context.filesDir/agent_data/<agentId>/`
- Default quota: 100MB per agent
- Keys are sanitized (`..`, `/`, `\` replaced with `_`)

### Android Architecture
- `AgentOSApplication` — initializes agents, holds FileStorage, manages chat history persistence, reads/writes API key from SharedPreferences
- `ChatViewModel : AndroidViewModel` — per-agent message lists, loads history on tab switch, saves after every exchange
- `ChatScreen` — observes ViewModel state flows, no direct agent access
- `isFirstLaunch` — true when `gemini_api_key` key is absent from SharedPreferences; shows `OnboardingScreen` instead of splash

### Security Rules
- **Never hardcode API keys in source.** JVM CLI reads from `GEMINI_API_KEY` env var only.
- Keystore credentials for Android release signing live in `local.properties` (gitignored), not `build.gradle.kts`.
- `local.properties` is never committed. CI injects credentials via secrets.

### Testing
- All tests use JUnit 5 (`org.junit.jupiter`) + `kotlin("test-junit5")`
- `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` is required in every module with tests
- Test the `aiChat()` fallback by using `apiKey = "test-key"` — GeminiClient will fail and return `"AI unavailable..."`
- Tests are in `core/src/test/kotlin/` mirroring the source package structure

---

## Existing Agents — Quick Reference

| Agent | Display Name | Storage Prefix | Key Commands |
|-------|-------------|---------------|--------------|
| NotesAgent | Notes | `note-` | `create note: title \| content`, `list notes`, `search notes <q>`, `get note <id>`, `delete note <id>` |
| CalendarAgent | Calendar | `event_` | `schedule <desc> on <date>`, `list events`, `what's on <date>`, `cancel event <id>` |
| TasksAgent | Tasks | `task_` | `add task: <title> [priority: high/medium/low] [due: <date>]`, `list tasks`, `complete task <id>`, `due today` |
| WeatherAgent | Weather | *(no storage)* | `weather <city>`, `forecast <city>` |
| EmailAgent | Email | `email_` | `compose email: to \| subject \| body`, `list inbox`, `read email <id>`, `reply to <id> \| body` |
| MessagingAgent | Messaging | `msg_` | `send message: contact \| body`, `list conversations`, `read conversation <contact>` |
| FinanceAgent | Finance | `txn_`, `budget_` | `add expense: desc \| amount \| cat`, `budget summary`, `spending this month`, `spending by category` |

---

## CI/CD

`.github/workflows/build.yml`:
- Triggers on push/PR to `main` and `develop`
- `gradle build -x test` → `gradle :core:test :sync:test`
- Tests are **not** `continue-on-error` — failures block the build
- Security check: `grep` for hardcoded `api_key`, `password`, `secret` patterns

---

## Git Workflow

- **Branch:** Always work on `claude/start-new-app-KOmLu`
- **Commits:** Descriptive messages with phase prefix (e.g. `Phase 9: ...`, `fix: ...`, `feat: ...`)
- **Push:** `git push origin claude/start-new-app-KOmLu` after each logical unit of work
- **Never** amend published commits

---

## What's Not Yet Built (Roadmap)

See `PRODUCT_STRATEGY.md` for full strategy. Immediate next phases:

- **Phase 10:** CalendarAgent recurring events; NotesAgent Markdown export; WeatherAgent GPS; FinanceAgent CSV import
- **Phase 11:** Real IMAP/SMTP for EmailAgent; Android SMS ContentProvider for MessagingAgent
- **Phase 12:** Cloud sync wired to agent storage; SyncServer auth layer
- **Phase 13:** Push notifications for reminders; Android widget; Play Store listing
- **Phase 14:** Agent marketplace browser (replace coming-soon dialog); developer SDK
