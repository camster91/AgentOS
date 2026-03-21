# Changelog

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
