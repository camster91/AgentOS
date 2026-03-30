# AgentOS

**AgentOS** is an open-source AI agent operating system for Android and JVM. Instead of a single monolithic assistant, it runs a fleet of specialized AI agents — each owning its own domain, storage, and conversation context — all powered by Google Gemini 2.0 Flash.

> **v0.3.0** · 7 built-in agents · Jetpack Compose · JVM CLI · Cloud sync

---

## What it does

A single chat UI routes your messages to the right agent automatically. Each agent understands its domain deeply, persists your data locally, and falls back to natural conversation via Gemini when a built-in command isn't recognized.

```
┌─────────────────────────────────────────────┐
│          Android App  (Jetpack Compose)      │
│   Notes · Calendar · Tasks · Weather · …    │
└──────────────────────┬──────────────────────┘
                       │
             ┌─────────▼──────────┐
             │   AgentOS Core     │
             │  (JVM / Kotlin)    │
             └──┬──┬──┬──┬──┬──┬─┘
                │  │  │  │  │  │  │
             Notes Cal Tasks Weather Email Msg Finance
                │  │  │  │  │  │  │
             ┌──┴──┴──┴──┴──┴──┴──┴──┐
             │  FileStorage (per-agent)│
             │  Gemini 2.0 Flash (AI)  │
             │  Ktor Sync Server       │
             └─────────────────────────┘
```

---

## Agents

| Agent | Key Commands |
|-------|-------------|
| **Notes** | `create note: title \| content` · `list notes` · `search notes <q>` · `get note <id>` |
| **Calendar** | `schedule <event> on <date> at <time>` · `list events` · `cancel event <id>` |
| **Tasks** | `add task: <title> [priority: high] [due: tomorrow]` · `list tasks` · `due today` |
| **Weather** | `weather London` · `forecast Tokyo` |
| **Email** | `compose email: to \| subject \| body` · `list inbox` · `reply to <id> \| body` |
| **Messaging** | `send message: Alice \| Hey!` · `list conversations` · `read conversation Alice` |
| **Finance** | `add expense: Coffee \| 4.50 \| Food` · `budget summary` · `spending by category` |

All agents fall back to Gemini conversation for unrecognized input.

---

## Quick Start

### Requirements
- JDK 17+
- Android Studio (for Android builds)
- [Gemini API key](https://aistudio.google.com) (free)

### JVM CLI
```bash
git clone https://github.com/camster91/AgentOS
cd AgentOS
export GEMINI_API_KEY=your_key_here
gradle :core:run --no-daemon
```

### Android
1. Open in Android Studio
2. Copy `local.properties.example` → `local.properties` and fill in your keystore details
3. Run on device or emulator — you'll be prompted for your Gemini API key on first launch

### Sync Server (optional)
```bash
gradle :sync:run --no-daemon
# Starts on http://localhost:8080
```

---

## Build

```bash
# Build JVM modules (no Android plugin required)
gradle :core:build :sync:build :agents:ai-core:build --no-daemon

# Run tests
gradle :core:test :sync:test --no-daemon

# Android APK (requires Android Studio / Google Maven)
gradle :android:assembleDebug --no-daemon
```

CI handles Android builds automatically on push.

---

## Modules

| Module | Description |
|--------|-------------|
| `:api` | Shared interfaces: `Agent`, `AgentScope`, `StorageAPI`, `ChatMessage` |
| `:agents:ai-core` | `GeminiClient` — Gemini 2.0 Flash via OkHttp REST |
| `:core` | JVM runtime: registry, storage, sandbox, all 7 agents, CLI |
| `:sync` | Ktor HTTP sync server + OkHttp sync client |
| `:android` | Jetpack Compose Android app — chat UI, settings, onboarding |

---

## Project Structure

```
AgentOS/
├── api/                     # Shared interfaces
├── agents/ai-core/          # GeminiClient
├── core/
│   └── src/main/kotlin/com/agentOS/
│       ├── core/            # AgentOS, AgentRegistry, EventBus, PermissionManager
│       ├── agents/          # 7 agent implementations
│       └── core/storage/    # FileStorage, InMemoryStorage
├── sync/                    # Ktor server + OkHttp client
└── android/
    ├── src/main/java/       # AgentOSApplication
    └── src/main/kotlin/     # ChatViewModel, Navigation, UI screens
```

---

## Configuration

### API Key
- **Android:** Enter on first launch, or update in Settings → Gemini API Key
- **JVM CLI:** `export GEMINI_API_KEY=your_key_here`
- Get a free key at [aistudio.google.com](https://aistudio.google.com)

### Release Signing (Android)
Copy `local.properties.example` → `local.properties`:
```properties
KEYSTORE_PATH=path/to/your.jks
KEYSTORE_PASSWORD=yourpassword
KEY_ALIAS=youralias
KEY_PASSWORD=yourkeypassword
```
`local.properties` is gitignored and never committed.

---

## Adding a New Agent

See [DEVELOPER.md](DEVELOPER.md) and [CLAUDE.md](CLAUDE.md) for the full guide. In short:

1. Create `core/src/main/kotlin/com/agentOS/agents/MyAgent.kt` extending `Agent()`
2. Register in `Main.kt` (JVM CLI) and `AgentOSApplication.kt` (Android)
3. Add display name to `ChatViewModel.agentList` and `SettingsScreen.allAgents`
4. Write tests in `core/src/test/kotlin/com/agentOS/agents/MyAgentTest.kt`

---

## Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Add tests — all new agents need a corresponding `*AgentTest.kt`
4. Run `gradle :core:test :sync:test --no-daemon` — must pass
5. Open a PR against `main`

See [DEVELOPER.md](DEVELOPER.md) for architecture details and [CLAUDE.md](CLAUDE.md) for AI-assisted development guidelines.

---

## Privacy

AgentOS stores all data locally on your device. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for full details.

---

## License

MIT License — see [LICENSE](LICENSE) for details.
