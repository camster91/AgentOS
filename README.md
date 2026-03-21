# AgentOS

**AgentOS** is an open-source AI assistant operating system for Android and JVM. It provides a modular, agent-based architecture where specialized AI agents handle different domains — notes, calendar, tasks, and weather — each powered by Google Gemini AI.

Unlike monolithic AI apps, AgentOS routes user requests to the right agent automatically, maintains per-agent conversation history, and syncs data across devices via a lightweight cloud sync backend. The Android app features a clean Jetpack Compose chat UI that feels like one unified assistant while running a fleet of specialized agents under the hood.

## Architecture

```
Android App (Jetpack Compose)
        │
        ▼
  AgentOS Core (JVM)
        │
   ┌────┴────────────────────────┐
   │    Agent Router             │
   └─┬──────┬──────┬────────────┘
     │      │      │      │
  Notes  Calendar Tasks Weather
     │      │      │      │
     └──────┴──────┴──────┘
              │
         Gemini AI
         (gemini-2.0-flash)
              │
         Cloud Sync
         (Ktor server)
```

## Quick Start

### Requirements
- JDK 17+
- Android Studio (for Android builds)
- Gradle 8.7+

### Build & Run (JVM)
```bash
git clone https://github.com/camster91/AgentOS
cd AgentOS
./gradlew :core:run
```

### Build Android APK
```bash
./gradlew :android:assembleDebug
# APK at: android/build/outputs/apk/debug/android-debug.apk
```

### Run Sync Server
```bash
./gradlew :sync:run
# Server starts on port 8080
```

## Agents

| Agent | Capabilities |
|-------|-------------|
| **NotesAgent** | Create, read, update, delete notes. AI-powered natural language management. |
| **CalendarAgent** | Schedule events, check availability, set reminders. Date-aware AI assistant. |
| **TasksAgent** | Manage to-dos, priorities, and deadlines. Productivity-focused AI. |
| **WeatherAgent** | Conversational weather queries via Open-Meteo API. |

## AI Integration

All agents use Google Gemini 2.0 Flash via the `agents/ai-core` module. Set your API key:

```bash
export GEMINI_API_KEY=your_key_here
```

Or pass it directly in code. Agents fall back to rule-based responses if the API is unavailable.

## Modules

| Module | Description |
|--------|-------------|
| `:core` | Agent runtime, storage, conversation memory, all 4 agents |
| `:agents:ai-core` | GeminiClient, ChatTurn data class |
| `:android` | Jetpack Compose chat UI, APK |
| `:sync` | Ktor sync server + OkHttp sync client |
| `:api` | Shared API interfaces |

## Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Add tests for new agents or features
4. Run `./gradlew build test` — must pass
5. Open a PR against `main`

See [DEVELOPER.md](DEVELOPER.md) for architecture details.
