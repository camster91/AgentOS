# Privacy Policy — AgentOS

**Last updated:** 2026-03-29

---

## Overview

AgentOS is designed with privacy as a core principle. Your data stays on your device.

---

## Data We Collect

**AgentOS does not collect, transmit, or share any personal data with the developers or any third party.**

All data created within the app — notes, calendar events, tasks, email drafts, messages, financial transactions — is stored exclusively on your device in the app's private storage directory (`filesDir`). This data is never sent to AgentOS servers.

---

## AI Processing (Google Gemini)

When you use conversational features, your messages are sent to **Google Gemini API** for processing. This is the only external service AgentOS communicates with.

- Your Gemini API key is stored locally in the app's SharedPreferences
- Message content sent to Gemini is subject to [Google's Privacy Policy](https://policies.google.com/privacy) and [Gemini API Terms of Service](https://ai.google.dev/terms)
- You can use AgentOS without an API key — AI responses will be unavailable, but all command-based features work offline

---

## Optional Cloud Sync

AgentOS includes an optional sync server (`sync` module). If you deploy and enable sync:
- Agent data is pushed to the sync server you control
- The official AgentOS app does **not** connect to any third-party sync service by default
- The sync server URL defaults to `localhost` and must be explicitly configured

---

## Permissions

| Permission | Reason |
|-----------|--------|
| `INTERNET` | Required to send messages to Gemini API and optional sync |

AgentOS does **not** request access to: contacts, SMS, camera, microphone, location, or any other sensitive permission in v0.3.0. Future versions may add optional permissions (e.g., GPS for weather) with explicit user consent.

---

## Data Storage

- All agent data is stored in `filesDir/agent_data/<agent>/` — private to the app
- Chat history is stored in `filesDir/chat_history/<agent>.txt`
- Your Gemini API key is stored in SharedPreferences (private to the app)
- No data is stored in external storage or synced to cloud without your explicit action

---

## Data Deletion

Uninstalling AgentOS removes all locally stored data. You can also clear data via Android Settings → Apps → AgentOS → Clear Data.

---

## Children

AgentOS is not directed at children under 13 and does not knowingly collect data from children.

---

## Changes

We may update this policy as features are added. The "Last updated" date at the top will reflect any changes.

---

## Contact

For privacy questions, open an issue at [github.com/camster91/AgentOS](https://github.com/camster91/AgentOS).
