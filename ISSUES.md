# Hackathon Issues

## Signed-in phone follow-up — latest

- Passed: real phone-side saved-login gateway round trip with registered list_apps and model continuation; no provider key embedded.
- Fixed: account labels and actionable, sanitized verification/credential errors.
- Passed after user service toggle and --no-restart instrumentation: synthetic message-send device test, including approval, recipient, generic-tap blocking and duplicate prevention. Accessibility remained bound with no crashed services afterward. No external message was sent. Real WhatsApp delivery and arbitrary-app semantics remain open in #3.
- Remaining production gates in GitHub #1–#6 are not superseded by this successful gateway test.

## Deployment and dynamic-runtime update — latest

- Supabase database migration, Edge Function and server-only model credentials deployed. Live smoke tests passed for Auth login, beta denial/approval, database grants, quota increments/denial and same-user/cross-user continuation. Temporary test accounts and provider responses removed.
- Removed app shortcut map, Spotify-only routing and exposed echo mock. Added installed-app discovery; model comes from deployment configuration, agent loop budget is configurable and bounded.
- Added experimental confirmed send_message with fresh draft/recipient/package checks; generic tap cannot send. English-labelled Send only, no arbitrary-app semantic or delivery guarantee. Real WhatsApp verification remains open.
- User confirmed Hey SOL invoked the assistant on this phone while unlocked. Lock-screen/OEM/long-duration evaluation remains open.
- Current local build is keyless gateway mode. No permanent SOL Auth user existed at deployment; create/verify a beta account in Setup, obtain owner approval, then sign in. Phone Auth/refresh and complete tool-loop qualification remain open.
- GitHub #1–#6 remain open where acceptance criteria still require human/device tests, privacy/release decisions, realtime architecture or deferred integrations. This pack does not falsely close those broader issues.

## Gateway hardening update — 2026-09-20

- Implemented: release-only gateway enforcement, email sign-in, Keystore-encrypted cross-process sessions, verified-user/beta access checks, request quotas, continuation ownership and bounded model requests. Added CI and gateway policy/auth rejection tests.
- Deployment blocked: Supabase CLI is not authenticated. Supplied publishable configuration is valid for a client but does not authorize deployment. No database changes or live gateway claims made. Developer demo mode remains enabled locally.
- Still open (#1/#4): apply and verify SQL/RLS under real service roles and concurrent load, deploy secrets/function, qualify sign-in/refresh/logout and multi-step tool use on the phone, review retention/privacy and spending controls. See docs/GATEWAY.md.
- Existing voice, screen control, integration and release backlog below remains open; this update does not claim 90% production readiness.

## Latest audit status — 2026-09-20

This section supersedes historical completion claims below. Source repairs are implemented; final combined build/device evidence is recorded in AUDIT.md. A checked historical milestone is not proof of fresh regression or production readiness.

- Repaired: automatic assistant dismissal; stale speech callbacks; missing conversation context; app cancellation/approval cleanup; repeating system-recognizer wake loop; competing microphone owners; mismatched accessibility traversals; stale observed targets; cancellation-blind queued accessibility actions; ambiguous app selection.
- Model: GPT-6 Astra is available on the configured account and a live Responses smoke request completed. Low reasoning and sequential tools are configured.
- Wake: offline Vosk model packaged. Human phrase/accent and long-running background tests remain necessary.
- Calls use the dialer; SMS uses drafts. Neither auto-calling nor auto-sending is claimed. Music search is supported; exact-track autoplay is not guaranteed. Maps/alarm intent dispatch is not proof of a completed real-world outcome.
- Not a production release: backend key protection, privacy review, multi-device validation and release qualification remain open.
- Final automated evidence: 29 unit tests, debug assembly and lint pass; APK installed. Fresh Astra Open Spotify passed with foreground verification. Settings opened, but screen observation correctly reported Accessibility disabled; the user must re-enable SOL screen control for live tap/type/scroll qualification. The offline model loaded and began continuous recording outside the app. Human wake and multi-turn speech tests remain open.

### GitHub follow-up backlog

| Remaining work | Tracking |
| --- | --- |
| Secure API gateway, authentication, quotas, screen privacy/retention | [#1](https://github.com/anandh0u/android-solappan/issues/1) |
| Low-power wake accuracy, battery, locked/OEM devices, audio routing | [#2](https://github.com/anandh0u/android-solappan/issues/2) |
| App-specific accessibility safety, adversarial tests, verified outcomes | [#3](https://github.com/anandh0u/android-solappan/issues/3) |
| Device regression, CI, signing, native alignment and store policy | [#4](https://github.com/anandh0u/android-solappan/issues/4) |
| Notifications/calendar/email/WhatsApp, exact music playback, configurable wake phrase, local LLM and reviewed tool expansion | [#5](https://github.com/anandh0u/android-solappan/issues/5) |
| Full-duplex voice, barge-in, encrypted optional history and state restoration | [#6](https://github.com/anandh0u/android-solappan/issues/6) |

Coordinate tapping, unrestricted swipes, arbitrary generated-code execution, payments and security/destructive automation are not implemented. The first two need separate reviewed designs; the latter capabilities remain excluded from this MVP.

## P0 — Submission Blockers

- [x] Android application builds successfully
- [x] Application launches without crashing
- [x] Text input works
- [x] OpenAI API connection works
- [x] API errors are handled
- [x] Tool calling works
- [x] Tool registry works
- [x] Unknown tools are rejected
- [x] Multiple tool calls can execute
- [x] `open_app` works
- [x] `open_maps` works
- [x] `set_alarm` works
- [x] `find_contact` works
- [x] `call_contact` works
- [x] Confirmation flow works
- [x] Agent execution state is visible
- [x] Demo Scenario 1 works
- [x] Demo Scenario 2 works
- [x] Demo Scenario 3 works
- [x] APK can be installed on demo phone

### SESSION1-001 — Configure and verify OpenAI API key

Priority:

P0

Problem:

The app has no `OPENAI_API_KEY` in the local, Git-ignored configuration.

Expected:

A real prompt reaches the Responses API and the model response appears in the UI.

Actual:

The app safely displays a configuration error without crashing.

Status:

COMPLETE — verified on the connected phone with a live Responses API request.

## P1 — Important Improvements

- [x] Prepare SMS tool
- [x] Better execution timeline
- [x] Cancellation
- [ ] Tool retry
- [x] Better error messages
- [x] Voice input
- [ ] Verification step
- [x] Loading/processing indicators
- [x] Improve confirmation UI
- [x] Demo reset functionality

### ASSISTANT-P1.1 — Quick Settings invocation

Priority:

P1

Problem:

Solappan had no reliable one-tap invocation path when a custom “Hey Sol” wake word was unavailable.

Expected:

A Quick Settings tile opens the same temporary system-assistant session without launching MainActivity or creating another agent pipeline.

Actual:

The manifest registers a native SOL `TileService`. Tapping it sends a package-internal request to the active `VoiceInteractionService`, which invokes the existing session with Android's `showSession` API. The APK builds, all tests pass, and a physical-device tile click produced the focused `VoiceInteractionSession` window.

Status:

COMPLETE

### ASSISTANT-P1.2 — Native media control

Priority:

P1

Problem:

The agent could open a music application but could not control the active playback session.

Expected:

Spoken play, pause, next, and previous requests pass through the model, registered tool, and Android native media APIs without accessibility automation.

Actual:

The allowlisted `control_media` tool validates four exact actions and dispatches matching Android media-key events through `AudioManager`. The build and all tests pass. A physical-phone voice request from the assistant successfully changed Spotify playback.

Status:

COMPLETE

### ASSISTANT-P1.3 — Explicit screen understanding

Priority:

P1

Problem:

The assistant could act on spoken goals but could not answer questions about the app visible underneath its temporary surface.

Expected:

An explicit screen question uses Android-provided context in the existing model pipeline, keeps unrelated commands text-only, and fails safely when capture is unavailable.

Actual:

The assistant requests Android assist data and supports both a compressed screenshot and bounded assist-structure text. Explicit screen phrases attach the ephemeral context to the first Responses API request; all other goals omit it. The phone delivered both sources over Wi-Fi Settings, and the user confirmed the spoken “What’s on my screen?” flow completed successfully. All screen context is discarded after the run.

Status:

COMPLETE

### ASSISTANT-P0.2 — Reusable agent execution state

Priority:

P0

Problem:

`MainActivity` directly mapped every `AgentEvent` into loading, timeline, result, and error UI state, which would force a system-assistant entry point to duplicate that orchestration.

Expected:

The normal app and future assistant session can consume the same UI-independent execution state without changing `AgentController` or the tool pipeline.

Actual:

`AgentRuntimeCoordinator` now owns event-to-state mapping while `MainActivity` remains responsible for Compose controls and confirmation presentation. Fourteen unit tests pass, the APK installs, and the existing “Open Spotify” workflow was verified on the connected phone after the extraction.

Status:

COMPLETE

### ASSISTANT-P0.3 — Android assistant role integration

Priority:

P0

Problem:

Solappan could only be launched as a normal application and was not eligible for Android's system assistant role.

Expected:

Android recognizes Solappan as a digital-assistant candidate, MainActivity reports current role status, and the user can open the system-controlled role selection flow.

Actual:

The manifest now registers protected voice-interaction and session services with Android voice-interaction metadata. The setup UI reports role availability and ownership, requests the role through `RoleManager`, and falls back to the OEM Digital Assistant settings screen when necessary. Android listed “Solappan Agent” as a candidate, the role was selected on the connected phone, and `dumpsys voiceinteraction` reports Solappan's service and session service as active. The assistant session UI is intentionally deferred to P0.4.

Status:

COMPLETE

### ASSISTANT-P0.4 — Temporary assistant session UI

Priority:

P0

Problem:

Android could start Solappan's assistant session, but the session had no visible interaction surface.

Expected:

Invoking the default assistant displays a small temporary SOL interface above the current app without launching MainActivity, and the user can dismiss it safely.

Actual:

`SolVoiceInteractionSession` now creates a compact dark bottom panel with SOL branding, a clear Ready state, and a Dismiss action. It was invoked through Android's ASSIST key path over the Nothing launcher; Android reported `VoiceInteractionSession` as the focused window, and Dismiss returned focus to the launcher. Speech is intentionally deferred to P0.5.

Status:

COMPLETE

### ASSISTANT-P0.5 — Assistant microphone and speech recognition

Priority:

P0

Problem:

The system assistant session could appear but could not listen for a natural-language request.

Expected:

MainActivity requests microphone permission through Android, assistant invocation starts speech recognition, recognized text is shown, and cancellation/no-speech/network errors offer a safe retry.

Actual:

Microphone setup and session-owned `SpeechRecognizer` integration are implemented. On the connected phone, Android permission grant, the active microphone indicator, Listening state, successful spoken transcription, no-match handling, and Retry UI were verified.

Status:

COMPLETE — the user manually verified successful spoken transcription on the connected phone.

### ASSISTANT-P0.6 — Send assistant transcript into existing agent

Priority:

P0

Problem:

The assistant session could transcribe speech but did not submit the recognized goal to the existing agent runtime.

Expected:

A final speech transcript enters `AgentRuntimeCoordinator`, `AgentController`, and `ToolRegistry` exactly like text input, with no command parser or second agent pipeline.

Actual:

The session now submits final transcripts into the shared runtime, shows basic execution state, displays the final result or safe error, and dismisses shortly after successful completion. The user spoke “Open Spotify”; two model requests completed, the registered `open_app` tool launched Spotify, and the assistant session dismissed automatically. Until P0.9 adds assistant confirmation UI, protected tools fail closed by returning a denied confirmation decision.

Status:

COMPLETE

### ASSISTANT-P0.7 — Assistant progress and tool timeline

Priority:

P0

Problem:

The assistant showed only a broad agent state and final response, so users could not see which registered Android tools were running or had completed.

Expected:

The assistant observes `AgentRuntimeUiState` and displays existing agent states plus a compact timeline for running, successful, failed, and cancelled tools.

Actual:

The native assistant panel now renders up to four recent entries from the shared runtime timeline with distinct state markers and colors. Transcript/result text is bounded to keep the temporary surface compact. The build and all tests pass, and the updated assistant session launches without crashing. A spoken tool run still needs one manual visual check because no speech reached the phone during the final automated test window.

Status:

WORKING — implementation is stable; live timeline visibility awaits manual confirmation.

### ASSISTANT-P0.8 — Existing tools from assistant invocation

Priority:

P0

Problem:

The existing Android tools had been verified from MainActivity, but needed device validation when the goal originated in the system assistant session process.

Expected:

Low-risk registered tools execute through the existing controller and registry, multi-tool workflows remain supported, and protected tools do not bypass missing assistant confirmation UI.

Actual:

Physical-device assistant tests passed for `open_app`, `set_alarm`, `open_maps`, and `find_contact`. The user spoke the alarm + GEC Thrissur goal; Android recent-task records confirmed both `ACTION_SET_ALARM` in Google Clock and Google Maps. Read-only contact lookup completed its model/tool round trip. A spoken call request completed as cancelled and returned to the launcher without opening the dialer, confirming fail-closed behaviour before P0.9.

Status:

COMPLETE

### ASSISTANT-P0.9 — Protected-action confirmation in assistant

Priority:

P0

Problem:

Protected call and SMS tools failed closed from assistant invocation because the temporary assistant surface could not collect an explicit user decision.

Expected:

The assistant displays the proposed action and risk, prevents click-through, supports cancellation and approval, and denies pending actions when its lifecycle ends.

Actual:

The native assistant panel now requires a 750 ms arming delay, a separate review checkbox, and an approval button. Cancelled call and SMS requests execute nothing; an approved call opens the system dialer. Approved SMS drafts target Android's default SMS package and bring its task forward while leaving Send under user control. Session hide, dismiss, retry, and destruction complete pending confirmations as denied. Registry enforcement remains unchanged and independent.

Status:

COMPLETE

### ASSISTANT-P0.10 — Final assistant tool validation

Priority:

P0

Problem:

The complete system-assistant path required a final physical-device pass across every demo-critical Android action before beginning P1 work.

Expected:

Assistant-originated requests reliably open an app, configure an alarm, open Maps, prepare a call after confirmation, and prepare an SMS draft after confirmation.

Actual:

The connected phone completed a combined spoken workflow that opened Spotify, launched Android's 7 AM alarm flow, and opened Maps for GEC Thrissur. Device task records confirmed all three intents. The call path passed cancellation and approval, opening the dialer only after explicit approval. The SMS path passed cancellation and approval; Android task records confirmed Google Messages received the `ACTION_SENDTO` request from Solappan and displayed the prepared conversation task. The user retained the final Call and Send actions.

Status:

COMPLETE

### SAFETY-001 — Prevent confirmation click-through

Priority:

P0

Problem:

A changing Compose layout could receive repeated activation at the confirmation button position during automated device testing.

Expected:

No protected tool can execute from a carried-over or repeated button activation.

Actual:

Protected tools now require a separately checked review control plus the approval button. The registry also rejects protected execution unless the controller supplies an explicit confirmation grant. Unit tests cover rejection and approved execution.

Status:

COMPLETE

### SESSION5-001 — Stabilization and demo validation

Priority:

P0

Problem:

The complete MVP required repeated device validation and recovery-path testing before submission.

Expected:

All three demo scenarios, approval/cancellation, missing resources, denied permission, malformed model output, and offline handling behave without crashes.

Actual:

All three demo scenarios passed on the connected phone. Failure paths produced structured, user-readable results. Automated tests pass and the APK installs successfully.

Status:

COMPLETE

## P2 — Stretch Goals

- [x] Screenshot capture through assistant context
- [x] Vision-based screen understanding
- [x] AccessibilityService implemented (manual enablement and live action validation pending)
- [x] Tap UI element implemented with confirmation and sensitive-action blocking
- [x] Type text implemented with confirmation and password-field blocking
- [x] Scroll screen implemented through semantic accessibility actions
- [ ] Read notifications
- [ ] WhatsApp experimental flow
- [ ] Additional system tools
- [ ] Codex-generated tool capability
- [ ] Dynamic tool installation
- [ ] Local model experimentation

### BUILD-001 — Align Android Gradle plugin with compile SDK 36

Priority:

P1

Problem:

Android Gradle Plugin 8.7.3 reports that it was tested through compile SDK 35 while this project compiles against SDK 36.

Expected:

The build uses an Android Gradle plugin and Gradle wrapper combination officially tested with compile SDK 36.

Actual:

Debug builds and tests succeed, but Gradle prints a compatibility warning.

Status:

COMPLETE — the project now compiles and targets SDK 35, matching the supported range of Android Gradle Plugin 8.7.3. The compatibility warning is gone.

## Current audit — 2026-09-20

Existing-scope runtime repairs are implemented: truthful intent failures and final state, strict argument validation, cancellation before execution, duplicate-run protection, assistant lifecycle/approval cleanup, readable persistent answers, bounded local screen context, safe multi-number contact handling, and guarded assistant-role APIs/broadcasts. Debug build, 18 unit tests and lint pass; APK installed. The nonexistent-app regression displays Failed on the phone.

OPEN VALIDATION: the updated assistant tile/gesture, speech retry, approved/cancelled drafts, and interruption during multi-step actions need a fresh manual device check. Injected assist-key testing was inconclusive. Earlier completion entries are historical evidence, not fresh validation of this audit build. See AUDIT.md.

AUDIT BASELINE NOTE: accessibility navigation/scrolling and custom wake activation were not implemented during the audit itself. Restricted accessibility was added later in P1.4. The experimental foreground Hey SOL listener was added in P2.1 and still needs final human phrase validation.

### ASSISTANT-P1.4 — Controlled accessibility and observation pack

Priority:

P1

Problem:

Apps without a native Android API or intent could be observed through assistant context but not controlled through registered tools.

Expected:

Accessibility remains optional, exposes only bounded registered actions, cannot approve its own confirmations or act on password/consequential labels, and supports observation after actions.

Actual:

The APK now registers an optional accessibility service and six tools: `observe_screen`, `tap_element`, `type_text`, `scroll_screen`, `press_back`, and `press_home`. Cross-process commands use signature-protected package broadcasts. Observations exclude SOL windows and password values and are bounded to 80 nodes. Tap/type require normal runtime confirmation; the service also blocks consequential labels. Native tools remain preferred in model instructions. Build, 20 unit tests, lint, installation, manifest registration, and setup UI passed. After user enablement, one physical-phone workflow opened Settings, observed `com.android.settings`, scrolled semantically, observed again, pressed Back, and pressed Home. A Chrome failure established that the full-screen voice-interaction window occluded the underlying app; retries alone could not fix it. The assistant now disables only its UI window during registered screen actions, restores it afterward, and selects only an active/focused foreign window. Matching is visible/enabled, exact-first, ambiguity-safe, password-aware, and checks sensitive resolved targets. A post-fix phone run opened Chrome, hid SOL during observation, completed the observation tool/model round trip without `SCREEN_UNAVAILABLE`, restored normal focus, and finished the session. The first approved Chrome tap then exposed a second transition race: the app root existed before its child nodes were republished. Element-not-found is now retried only when no action executed, with a longer UI-yield delay. Live tap/type validation remains OPEN.

Status:

WORKING — observe/scroll/Back/Home passed on-device; tap/type confirmation tests remain.

### ASSISTANT-P1.5 — Spotify query routing

Priority:

P1

Problem:

Native media keys could resume or pause an active session but could not take a music query to Spotify results.

Expected:

A registered deterministic tool opens Spotify search for a user-provided query without falsely claiming playback.

Actual:

`search_music(query, provider="spotify")` uses Spotify's native search URI with a browser fallback. The schema rejects other providers and extra arguments. Model instructions keep search and playback/verification distinct. Build, tests, lint, and installation pass. A physical-device request for Starboy opened Spotify and the visible Spotify screen title showed `Starboy`; no autoplay claim was made.

Status:

COMPLETE — Spotify query routing and visible result-page validation passed on-device.

## Blocking Rule

Do not implement P2 work while major P0 issues remain.

### ASSISTANT-P2.1 — Spoken responses, Hey SOL, and minimal app surface

Priority:

P2

Problem:

SOL could accept speech but returned final answers silently, required a manual system gesture/tile for every invocation, and exposed setup/debug cards as the primary app experience.

Expected:

Final answers are spoken, the app defaults to a clean conversation surface, and an explicitly enabled foreground wake listener can invoke the existing assistant without creating another agent pipeline.

Actual:

Android TTS is integrated into both entry surfaces. MainActivity now shows a custom SOL app icon/mark, conversation, Mic/Send controls, wake toggle, and collapsed Setup panel. `SolWakeWordService` runs as a microphone foreground service, recognizes four SOL phrases, invokes the existing `VoiceInteractionService`, and coordinates microphone ownership with the assistant. The service never sends continuous microphone audio to OpenAI. Phrase matching has unit coverage. Build, lint, 22 tests, manifest registration, minimal UI hierarchy, service foreground state, notification, and offline recognizer startup pass. A typed model response rendered correctly and Android's TTS audio path became active. A final physical spoken wake phrase and assistant-surface audio check remain OPEN.

Status:

WORKING — implemented and installed; final human wake phrase and assistant-audio validation pending.

## Issue Format

New issues should follow:

### ISSUE-ID — Name

Priority:

P0 / P1 / P2

Problem:

Description.

Expected:

Expected behaviour.

Actual:

Observed behaviour.

Status:

OPEN / WORKING / BLOCKED / COMPLETE
