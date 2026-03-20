# AgentOS

A conversational, fail-proof operating system where AI agents replace traditional apps. Everything is configured through chat, scoped to prevent breaking, and guaranteed to work offline.

## Vision

AgentOS reimagines personal computing for the AI era:
- **Apps → Agents**: Conversational assistants instead of traditional applications
- **Fail-Proof by Design**: Agents can't break themselves, each other, or the OS
- **Chat-First**: Configure everything through conversation, not menus
- **Private & Owned**: Local-first, data stays on your device
- **Extensible**: Agent marketplace for community-built agents

## Core Features

- **Sandboxed Agent Runtime**: WebAssembly-based execution with hard resource limits
- **Capability Permissions**: Fine-grained control—agents declare what they need
- **Immutable Core OS**: The foundation can't be modified or corrupted
- **Conversational Configuration**: "Show me weekly budget summaries" instead of settings
- **Agent Marketplace**: Discover, install, and rate agents from creators worldwide
- **Multi-Device Sync**: Seamless experience across phone, tablet, and web (coming soon)

## Built-in Agents (MVP)

- **Email Agent** — Gmail alternative, conversation-driven inbox
- **Calendar Agent** — Natural language event creation and scheduling
- **Task Agent** — Project management through conversation
- **Notes Agent** — Smart linking, local-first storage
- **Weather Agent** — Simple, no permissions needed
- **Finance Agent** — Conversational budgeting (view-only, never auto-executes)
- **Messaging Agent** — Unified SMS/RCS/IM, privacy-first

## Getting Started

### For Users
Download AgentOS from the Play Store (coming soon). Install agents from the marketplace. Chat with them.

### For Developers

**Requirements:**
- Kotlin 1.9+
- Android 14+ SDK
- Gradle 8.0+
- Java 17+

**Setup:**
```bash
git clone https://github.com/camster91/AgentOS.git
cd AgentOS
./gradlew build
```

**Architecture:**
- `/core` — OS kernel (immutable)
- `/runtime` — WebAssembly sandbox
- `/agents` — Built-in agents
- `/api` — Agent interaction API
- `/ui` — Touch-first Android UI
- `/security` — Permissions & sandboxing
- `/storage` — Local + cloud sync
- `/docs` — Developer guides

See `DEVELOPER.md` for building custom agents.

## Security Model

AgentOS uses **capability-based security**:
- Agents declare required capabilities (Contacts, Calendar, Storage, etc.)
- Users grant specific access at install time
- Runtime enforcement prevents access beyond granted capabilities
- Agents cannot modify themselves or the OS core
- Resource limits prevent monopolization (100MB storage, 5% CPU, 200MB RAM per agent)

## Roadmap

**Phase 1 (6 months):** MVP agents + core framework + Android beta
**Phase 2 (12 months):** Agent marketplace + 50+ community agents
**Phase 3 (18 months):** iOS launch + financial integrations
**Phase 4 (2 years):** $1M+ ARR, 100K users, ecosystem growth

## Why AgentOS?

| Feature | iOS App Store | Android | AgentOS |
|---------|---|---|---|
| Fail-proof | ❌ Apps break | ❌ Apps break | ✅ Guaranteed safe |
| Privacy | ⚠️ Limited | ⚠️ Limited | ✅ Local-first |
| Customization | ❌ Limited | ✅ High | ✅ Via conversation |
| Technical knowledge | ❌ Required | ⚠️ Some | ✅ None |
| Offline | ⚠️ App-dependent | ⚠️ App-dependent | ✅ Always works |

## Contributing

We're building the future of personal computing. Contribute agents, improve the core, or help with documentation.

See `CONTRIBUTING.md` (coming soon) for guidelines.

## License

[TBD — likely AGPLv3 or similar open-source]

## Contact

- **GitHub**: github.com/camster91/AgentOS
- **Discord**: [link coming]
- **Email**: cameron@ashbi.ca

---

**AgentOS: Computing that just works.**
