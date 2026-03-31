# AgentOS (GlowOS Mobile)

**AgentOS** is the open-source AI agent operating system for Android and JVM. Serving as the mobile portal for the GlowOS ecosystem, it brings specialized, on-device AI agents to your pocket.

## 📱 The Mainstream Agent Experience

We are positioning AgentOS as the primary consumer Android app for the GlowOS suite. Instead of overwhelming users with terminal prompts, AgentOS provides a clean, native interface that seamlessly routes natural language to the correct agent backend (whether it's managing local notes or executing remote coding tasks via the GlowOS Engine).

**Key Features:**
- **Native Android UI:** Built entirely in Jetpack Compose for fluid, native performance.
- **Fleet of Agents:** Notes, Calendar, Tasks, Weather, Finance, and soon... the remote Pi Coding Agent.
- **Local Persistence:** Data is stored locally per-agent, ensuring privacy and speed.
- **Gemini Powered:** Backed by Gemini Flash for lightning-fast NLP routing and conversation fallback.

## 🚀 Release Preparation & Store Submission

This application is currently in the **Future Planning & Launch** pipeline for the Google Play Store. 

**Upcoming Launch Tasks:**
- [ ] Finalize app icon and Play Store metadata (screenshots, descriptions).
- [ ] Connect the remote GlowOS Broker API to enable cloud-side coding agent access from the phone.
- [ ] Complete the ProGuard/R8 release build optimization.
- [ ] Sign the Android App Bundle (AAB) for production submission.

## 🏗 Architecture

```text
┌─────────────────────────────────────────────┐
│          Android App  (Jetpack Compose)      │
│   Notes · Calendar · Tasks · Coding · …     │
└──────────────────────┬──────────────────────┘
                       │
             ┌─────────▼──────────┐
             │   AgentOS Core     │
             │  (JVM / Kotlin)    │
             └──┬──┬──┬──┬──┬──┬─┘
                │  │  │  │  │  │  │
             Notes Cal Tasks GlowOS Email Msg Finance
                │  │  │  │  │  │  │
             ┌──┴──┴──┴──┴──┴──┴──┴──┐
             │  FileStorage (per-agent)│
             │  Gemini Flash (Routing) │
             │  Ktor / WebSockets      │
             └─────────────────────────┘
```

## 🛠 Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- JDK 17
- A Gemini API Key

### Setup
1. Clone this repository.
2. Open the project in Android Studio.
3. Copy `local.properties.example` to `local.properties` and add your API keys:
   ```properties
   GEMINI_API_KEY=your_key_here
   ```
4. Build and run on an emulator or physical Android device.

## 📜 License & Roadmap
Part of the Nexus AI ecosystem. Next steps include migrating fully to the 24-hour token reset model for mainstream consumers, ensuring scalable backend costs for heavy usage.
