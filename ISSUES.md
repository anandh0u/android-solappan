# Hackathon Issues

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

Microphone setup and session-owned `SpeechRecognizer` integration are implemented. On the connected phone, Android permission grant, the active microphone indicator, Listening state, no-match handling, and Retry UI were verified. A successful spoken transcript still requires one manual voice test because synthesized audio from the development computer was not captured by the phone. Recognized text is intentionally not sent to the agent until P0.6.

Status:

WORKING — successful transcript display awaits manual speech verification.

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

- [ ] Screenshot capture
- [ ] Vision-based screen understanding
- [ ] AccessibilityService
- [ ] Tap UI element
- [ ] Type text
- [ ] Swipe
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

## Blocking Rule

Do not implement P2 work while major P0 issues remain.

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
