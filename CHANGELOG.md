# Changelog

## [v0.3.0] — 2026-03-28

### Added
- **Email Agent** — Compose, inbox/sent, read, reply, search, delete, mark-read
- **Messaging Agent** — Universal inbox: send, conversations, search, contacts
- **Finance Agent** — Track expenses/income, category budgets, monthly summaries
- **FileStorage on Android** — Agent data now persists across app restarts
- **API key settings** — Configure Gemini API key in-app (stored in SharedPreferences)
- **Per-agent chat history** — Switching agent tabs restores the correct conversation
- **Marketplace dialog** — "+" button opens coming-soon marketplace notice
- **33 new tests** — EmailAgentTest, MessagingAgentTest, FinanceAgentTest (11 each)

### Fixed
- JUnit Platform loading failure — replaced `kotlin("test")` with `kotlin("test-junit5")`
  and added `junit-platform-launcher` runtime dependency across all test modules
- CI no longer suppresses test failures with `continue-on-error: true`

### Changed
- Android `ChatViewModel` wired to real agents (was returning stub responses)
- `AgentOSApplication` initializes all 7 agents with `FileStorage`
- Settings screen updated: all 7 agents listed, API key field, version v0.3.0

## [v0.2.0] — 2026-03-21

### Added
- **Gemini AI integration** — All 4 agents (Notes, Calendar, Tasks, Weather) now powered by Gemini 2.0 Flash
- **agents/ai-core module** — GeminiClient with conversation history support
- **ConversationMemory** — Per-agent history persisted across restarts (max 50 turns)
- **Cloud sync backend** — Ktor HTTP server with push/pull endpoints
- **SyncClient** — OkHttp-based client for cross-device sync
- **Signed release APK** — R8 minification, production-ready signing

### Changed
- All agents fall back to rule-based logic if Gemini API is unavailable
- Android app now has proper Application class (AgentOSApplication)

## [v0.1.0-android-alpha] — 2026-03-20

### Added
- Android shell with Jetpack Compose chat UI
- Debug APK (16.5MB)
- Agent routing from Android UI to JVM core
- GitHub Actions CI/CD pipeline

## [v0.1.0-alpha] — 2026-03-19

### Added
- JVM core with 4 working agents: NotesAgent, CalendarAgent, TasksAgent, WeatherAgent
- FileStorage for persistent key-value data
- AgentRegistry with intent-based routing
- EventBus for agent communication
- Initial test suite
