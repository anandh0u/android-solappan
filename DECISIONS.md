# Architecture Decision Log

## ADR-031 — Dynamic app discovery and confirmed message sending

Remove app-package shortcut tables and Spotify-only routing. Resolve real installed labels and expose list_apps; use the standard media search/play intent or observed UI when unsupported. Server model selection moves to OPENAI_MODEL, while safety caps and registered capabilities remain enforced. Remove echo from the phone registry to match gateway policy.

The dedicated experimental send_message tool, unlike generic tapping, may dispatch Send after explicit approval. It requires a fresh unchanged observed package, exact draft, unique visible recipient above that draft, and an English-labelled Send button. It is one-shot and never retries uncertainty. This is deliberately limited UI evidence, not universal semantic safety, verified recipient identity or delivery assurance. Lock-screen screen actions are rejected. Cross-app qualification and adversarial evaluation remain open.

Supabase migration/function/secrets are now deployed, with live access-control and continuation smoke evidence. The current local build uses gateway mode with no embedded provider key; user onboarding and owner beta approval are required before phone reasoning.

## ADR-030 — Authenticated private-beta gateway

Release builds always route through Supabase and exclude the developer OpenAI credential. Debug direct mode remains available only to preserve the private demo until deployment. Email/password Auth sessions are AES-GCM encrypted with Android Keystore in no-backup storage, with a cross-process refresh lock; passwords are not persisted. The Edge Function independently verifies Auth tokens, requires verified allowlisted users, fixes model cost parameters and validates continuation ownership. SQL security-definer quota reservation uses a transaction advisory lock for small-beta throughput, with service-role-only execution. This is a private-beta design, not a public scale or privacy certification. Hosted deployment and device account validation remain release gates.

## ADR-029 — Audit corrections and current behavior (2026-09-20)

Supersedes the auto-dismiss portions of ADR-017 and recognizer-loop implementation of ADR-027. The assistant stays visible after completion until Close SOL, an explicit spoken dismissal, or system/user dismissal. One post-TTS follow-up recognition turn is offered; silence does not restart the recognizer. Three bounded text turns provide session-local context, never authorization or stale screen evidence.

Wake detection uses bundled Vosk 0.3.75 with the Apache-2.0 small US English model, continuous on-device audio, exact final phrases, and a visible opt-in microphone foreground service. Independent app/session pause owners prevent TTS feedback. Ordered pause broadcasts acknowledge microphone release before manual assistant recognition. Failed launches stop rather than triggering repeated popups. This is not a hardware low-power hotword guarantee.

The model default is GPT-6 Astra, low reasoning effort, 4096 output-token ceiling and sequential function calls. Account availability and a live Responses request were verified. Local configuration may override this; no secrets enter Git. Public APK distribution still requires the secure gateway in GitHub issue #1.

Accessibility observation and matching now share a 512-node/32-depth traversal. Tap/type require recent observed identity, bounds and window; changes invalidate targets. Commands carry cancellation leases and deadlines; controller execution is interruptible. Already-dispatched Android effects cannot be reversed. Label guards are heuristic, not a semantic security proof (issue #3).

## ADR-001 — Native Android

Status: Accepted

The project will be implemented as a native Android application.

## ADR-002 — Kotlin

Status: Accepted

Kotlin is the main development language.

Do not migrate languages during the hackathon.

## ADR-003 — Jetpack Compose

Status: Accepted

Jetpack Compose will be used for the primary interface.

Do not replace Compose with XML layouts unless an Android integration absolutely requires it.

## ADR-004 — Tool-Based Agent Architecture

Status: Accepted

The language model never receives unrestricted device control.

Android capabilities are exposed through explicit registered tools.

Flow:

Model

↓

Tool request

↓

Tool Registry

↓

Validation

↓

Execution

## ADR-005 — Native First

Status: Accepted

Implementation priority:

1. Native Android API
2. Android Intent
3. Official application/API integration
4. Accessibility/UI automation

## ADR-006 — Accessibility Is Optional

Status: Accepted

AccessibilityService must not be required for the core MVP.

It is a stretch capability.

## ADR-007 — Human Approval

Status: Accepted

Consequential actions require explicit user confirmation.

## ADR-008 — Single Main Agent UI

Status: Accepted

Do not build a complex multi-screen application.

The hackathon demo should primarily use one polished agent interface.

## ADR-009 — Reliability Before Feature Count

Status: Accepted

Do not add tools simply to increase the feature count.

Core tools must remain stable.

## ADR-010 — No Automatic Generated-Code Execution

Status: Accepted

Even if Codex-generated capability creation is implemented later, generated code must not automatically receive unrestricted execution privileges.

## ADR-011 — Keep Contact Numbers Inside the Android Runtime

Status: Accepted

Contact search results sent back to the model contain only an internal contact ID and display name. Raw phone numbers remain inside the Android runtime and are resolved only when constructing a dialer or SMS intent.

Calls use `ACTION_DIAL` rather than direct calling, and messages use an `ACTION_SENDTO` draft. The system application retains the final Call or Send action as defense in depth; the dedicated in-app approval flow remains a Session 4 requirement.

## ADR-012 — Defense-in-Depth Confirmation

Status: Accepted

Consequential tools require two independent runtime conditions: the user checks a review control and taps the approval action, then `ToolRegistry` receives an explicit confirmation grant from `AgentController`.

Calling a protected tool through the registry without that grant returns `CONFIRMATION_REQUIRED` and never reaches the Android intent.

## ADR-013 — Shared Runtime State Above AgentController

Status: Accepted

`AgentController` remains the single reasoning and tool-execution pipeline. `AgentRuntimeCoordinator` converts its events and final result into UI-independent runtime state that can be consumed by both `MainActivity` and a future Android assistant session.

Confirmation presentation remains entry-point-specific, but both entry points must pass the resulting user decision into the same controller callback and registry enforcement path.

## ADR-014 — Android Voice Interaction as the System Entry Point

Status: Accepted

Solappan uses Android's supported `VoiceInteractionService`, `VoiceInteractionSessionService`, and `VoiceInteractionSession` APIs instead of a permanent overlay or Accessibility service for assistant invocation.

The always-available interaction service and temporary session service run in dedicated application processes. MainActivity remains the setup/debug surface and requests the assistant role through the system-controlled `RoleManager` flow, with the OEM Digital Assistant settings page as a fallback.

## ADR-015 — Lightweight Native Assistant Surface

Status: Accepted

The system assistant session uses a small native Android view hierarchy created by `VoiceInteractionSession` rather than launching MainActivity or maintaining a permanent overlay. This keeps invocation fast, lifecycle ownership explicit, and dependencies minimal.

The normal Compose screen remains the richer setup/debug experience. Both surfaces will consume the same runtime coordinator once speech and agent execution are connected.

## ADR-016 — Android SpeechRecognizer for Session Input

Status: Accepted

The assistant session uses Android's `SpeechRecognizer` with explicit `RECORD_AUDIO` permission rather than a second model-facing voice pipeline. Partial and final transcripts stay inside the assistant session and will enter the existing `AgentController` pipeline as text.

MainActivity remains responsible for explaining and requesting microphone permission. The session handles missing permission, unavailable recognition, silence, cancellation, network failure, and retry without crashing.

## ADR-017 — Assistant Reuses the Existing Agent Pipeline

Status: Accepted

Final assistant transcripts are submitted to the same `AgentRuntimeCoordinator`, `AgentController`, and `ToolRegistry` used by MainActivity. No keyword command parser or alternate voice-agent logic is permitted.

Protected tool requests from the assistant fail closed until the assistant-specific confirmation UI is implemented. Low-risk completed workflows dismiss the temporary session after a short visible completion delay.

## ADR-018 — Assistant Confirmation Fails Closed

Status: Accepted

The assistant session presents protected tool requests inside its native temporary surface. Approval requires a separately enabled review checkbox followed by the approval button, with a short arming delay to prevent click-through.

Hiding, dismissing, restarting, or destroying the session resolves any pending decision as denied. An approved decision still passes through `AgentController` and the independent `ToolRegistry` confirmation gate. Calls open `ACTION_DIAL` and SMS actions open a draft in Android's default messaging package; the user retains the final Call or Send action.

## ADR-019 — Quick Settings Uses the Active Voice Service

Status: Accepted

The SOL Quick Settings tile sends an app-internal, non-exported broadcast to the active `VoiceInteractionService`, which calls Android's supported `showSession` API. This reliably opens the existing assistant session on the Nothing phone without a chooser, permanent overlay, duplicate agent pipeline, or background microphone.

The tile is the reliable one-tap alternative to a custom wake word. Long-press power remains the primary system gesture.

## ADR-020 — Native Media Commands Before App Automation

Status: Accepted

Music playback uses Android's `AudioManager` media-key dispatch rather than Spotify-specific UI automation. The registered `control_media` tool accepts only `play`, `pause`, `next`, and `previous`, rejects other arguments, and remains low risk.

This keeps media control independent of a particular player and preserves the native-first architecture.

## ADR-021 — Screen Context Is Explicit and Locally Ephemeral

Status: Accepted

Screen understanding uses the context Android supplies to the active `VoiceInteractionSession`: a screenshot when available and assist-structure text as a native fallback. SOL attaches this context only when the spoken goal explicitly asks about the screen, limits and compresses it, treats extracted UI text as untrusted data, and discards it after one run.

Image input is added only to the initial request of the existing Responses API and `AgentController` flow. If Android supplies neither source, the session reports that screen context is unavailable rather than inventing an answer. No MediaProjection, Accessibility service, permanent capture, or second model pipeline is introduced.

Local clearing does not imply deletion from the API provider. Screenshot contents may include sensitive visible information; filtering assist-text password fields is not screenshot redaction.

## ADR-022 — Dismissal Cancels Remaining Assistant Work

Status: Accepted

Hiding or restarting the assistant cancels its in-flight run and denies pending approval. The controller propagates cancellation and checks before executing another registered tool. Android actions already dispatched cannot be reversed. Multi-step workflows interrupted by dismissal require a new user request; the updated lifecycle still needs a fresh manual device regression check.

## ADR-023 — Accessibility Is an Optional, Restricted Fallback

Status: Accepted

SOL exposes bounded accessibility observation, semantic element taps, editable-field text replacement, semantic scrolling, Back, and Home only as registered tools. Commands cross the assistant/main process boundary through package-scoped broadcasts protected by the app's signature permission. Native APIs and intents remain preferred.

Tap and type use the existing confirmation gate. The accessibility service independently rejects SOL's own windows, password fields, and consequential targets such as approval, calls, sending, purchases, permissions, installation, and deletion. Observations are limited to 80 nodes and treat all screen text as untrusted. The service is optional and requires explicit Android Settings consent; disabled service errors are structured and do not affect the core MVP.

This is observation-based verification, not proof of real-world outcomes. It can verify visible package/UI state after an action, but cannot prove that navigation started, a payment settled, a message sent, or another external effect completed.

## ADR-024 — Assistant UI Yields During Screen Actions

Status: Accepted

Android implements a voice-interaction session as a full-screen window even when SOL draws only a compact panel. While that window is enabled, OEM accessibility exposes the SOL window and can omit the underlying application entirely. Waiting or retrying cannot recover a window that remains occluded.

Immediately before a registered accessibility tool executes, the session temporarily disables only its UI window and waits briefly for Android to publish the underlying app. The agent coroutine and session remain alive. Confirmation is collected before this transition, and the panel is restored as soon as the tool finishes or the run reaches a terminal state. This does not weaken `ToolRegistry` confirmation enforcement.

## ADR-025 — Music Search Is Honest Intent Routing

Status: Accepted

`search_music` opens Spotify search results through its native URI with a web fallback. It does not claim autoplay. Playback control remains a separate native media command, and visible screen observation may be used to verify only what the UI actually shows.

## ADR-026 — Spoken Replies Use Android Text-to-Speech

Status: Accepted

SOL speaks final model responses through Android's installed text-to-speech engine in both the Compose app and the system-assistant session. Speech is capped to a bounded response length, stops with the owning surface, and does not create a second agent pipeline. This is post-response speech output, not full-duplex OpenAI Realtime audio.

## ADR-027 — Hey SOL Is an Explicit Foreground Feature

Status: Experimental

Custom wake listening runs only after the user enables it from the SOL header. Android requires a visible foreground-service notification and microphone permission. Speech recognition checks locally/system-side for `Hey SOL`, `Hello SOL`, `Okay SOL`, or `OK SOL`; continuous microphone audio is not sent to OpenAI.

The wake recognizer pauses while the assistant session owns speech input or TTS and resumes after the assistant hides. Android's assistant gesture and Quick Settings tile remain the reliable invocation paths. A production version should replace this recognizer loop with a low-power dedicated on-device hotword engine.

## ADR-028 — Minimal Chat Surface, Collapsed Setup

Status: Accepted

MainActivity now defaults to a SOL-branded conversation surface with microphone, input, Send, wake status, and a single Setup entry. Permission, role, and accessibility details remain available in the collapsed Setup panel but no longer dominate the product experience. The custom SOL mark avoids copying OpenAI product logos or branding.
