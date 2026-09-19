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
