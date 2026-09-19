# SOL — Agent Runtime for Android

**Track 04 — Next-Gen Productivity & Automation**

## Pitch

SOL turns a spoken or typed goal into a controlled Android workflow. A reasoning model selects registered tools; the Android runtime validates arguments, collects approvals, performs actions, and returns structured results.

> “Set an alarm for 7 AM, navigate to GEC Thrissur, and prepare a message to Afnan saying I’ll meet him there.”

One request connects multiple apps while keeping consequential actions under user control.

## What we built

- One shared agent pipeline for chat, Android system-assistant invocation, and optional Hey SOL.
- Fourteen device tools: eight native/intent integrations and six optional accessibility tools.
- A persistent assistant panel with spoken replies, follow-up listening, and explicit Close SOL.
- Three recent text turns of in-memory context for follow-up requests.
- Offline Vosk wake recognition, Android command recognition/TTS, and GPT-6 Astra reasoning.
- A strict registry, bounded execution, parameter validation, explicit approval, cancellation, and visible failure results.

Calls open the dialer and messages open drafts. The user performs the final Call or Send action. Screen observations describe visible state; they do not prove real-world completion.

## Demonstration

1. “Open Spotify.”
2. “Set an alarm for 7 AM and navigate to GEC Thrissur.”
3. “Prepare a message to Afnan saying I will reach 20 minutes late.”
4. Ask a follow-up, then say “Close SOL” during listening.

Use the [demo script](DEMO_SCRIPT.md) and the [current audit](AUDIT.md) to select qualified paths. Accessibility tap/type and the replacement wake implementation require fresh physical-device qualification before being presented as verified.

## Why this fits Track 04

| Track theme | SOL implementation |
| --- | --- |
| Agent | A high-level goal drives iterative tool selection and result interpretation |
| Automation | Multiple Android actions share one request and execution timeline |
| Tools | Android APIs, intents, and restricted screen actions are registered capabilities |
| Productivity | Less manual app switching, repeated input, and context copying |

Codex was used to build and iteratively debug the Android runtime, integrations, and tests. The runtime exposes a controlled interface to the model; it never automatically executes model-generated code.

## Evidence and limits

The default model passed live text and structured-tool API checks. The combined debug build, 29 unit tests, lint and installation passed. Offline wake startup passed on the phone; human phrase and broader workflow checks remain open in [AUDIT.md](AUDIT.md). Earlier successful demos are historical evidence, not certification of the current build.

This is a hackathon technical alpha. Production requires an authenticated model gateway, OEM wake/battery qualification, accessibility assurance, lifecycle/realtime work, and release/privacy hardening. The [GitHub roadmap](https://github.com/anandh0u/android-solappan/issues) tracks that work.

## Technology

Kotlin · Jetpack Compose · Android VoiceInteractionService · Native APIs and intents · Optional AccessibilityService · Vosk Android 0.3.75 · Android speech recognition/TTS · OpenAI Responses API
