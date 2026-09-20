# Current audit — 2026-09-20

## Signed-in phone verification — latest

- Saved encrypted SOL login completed a real phone-side gateway → registered list_apps → model continuation test. The test asserted gateway mode and an empty embedded provider key.
- Account UI no longer labels login as beta; known authentication errors distinguish unverified email from invalid credentials without exposing raw server messages.
- Android unit tests, debug APK, instrumentation APK and lint build passed after these changes.
- Synthetic send test PASSED after the user toggled screen control off/on and instrumentation ran with --no-restart. It verified observation, approval-required rejection, generic Send-tap rejection, wrong-recipient rejection, approved synthetic send and duplicate prevention. SOL remained bound afterward with no crashed accessibility services. No external message was sent; real WhatsApp compatibility/delivery remains unverified.
- Test-harness lesson: ordinary instrumentation force-stops SOL and can leave accessibility unbound. On this API 36 phone use `adb shell am instrument --no-restart -w -e class com.solappan.agent.testing.MessageSendDeviceTest com.solappan.agent.test/androidx.test.runner.AndroidJUnitRunner` with SOL already bound and the test's accessibility-preservation flag. This does not establish behavior on other Android versions.
- This is a tested gateway-backed MVP, not a completed production release. Existing GitHub release gates remain open.

## Deployed gateway and dynamic runtime — newest evidence

- Supabase migration and Edge Function deployed to the linked project. OPENAI_API_KEY and OPENAI_MODEL configured only on the server. Live smoke tests passed for login, beta rejection/approval, client table/RPC denial, model response, continuation ownership, quota increments and daily quota exhaustion. Temporary test accounts and provider responses deleted after testing.
- Current debug and release generated configuration both exclude the provider key; the phone build now requires a verified, approved SOL account. No permanent app account existed when tested. Signup UI added; actual user onboarding/phone refresh remains pending.
- Removed exposed echo mock (which previously caused gateway schema rejection), app package shortcut list and Spotify-only URI routing. Added dynamic installed-app discovery, standard media search/play routing and configurable bounded loop budget.
- Added dedicated experimental confirmed send_message with recipient/draft/package/observation binding and one-shot dispatch. Generic tap Send remains blocked; locked screen roots are rejected. Unit policy tests pass; semantic assurance across arbitrary apps is NOT established.
- Combined Android unit/debug/instrumentation assembly/lint build passed with 37 unit tests; 10 Deno tests pass. Updated APK installed. The device instrumentation test FAILED at observation: Android listed SOL Accessibility as enabled but crashed/unbound. It did not reach Send and did not contact anyone. User must toggle SOL screen control off/on before retesting. Do not present this feature as device-qualified.
- User confirmed unlocked spoken Hey SOL invocation in this conversation. This does not establish screen-off/OEM accuracy or battery performance. Full-duplex voice, durable history and deferred productivity integrations remain open GitHub work.

Older sections below describe earlier revisions and are superseded by this section.

## Gateway increment — latest evidence

- `testDebugUnitTest assembleDebug lintDebug generateReleaseBuildConfig`: successful; 31 Android tests pass. Lint retains 31 warnings, no errors.
- `deno test supabase/functions/agent-gateway`: 10 policy and mocked authentication/ownership tests pass.
- Generated release configuration verified: empty provider key and gateway mode enabled.
- Updated debug APK installed successfully; MainActivity cold-launch returned Status: ok on the connected phone. This is not a voice or cross-app regression test.
- Supabase Auth settings endpoint reachable; CLI lacks deployment authentication. No schema/function deployed. Local PostgreSQL qualification unavailable because Docker engine is stopped. Database grants, quota concurrency, real sign-in/refresh and gateway tool workflows remain unverified.
- CI added; hosted result must be checked separately. Deployment/retention instructions are in docs/GATEWAY.md; remaining release gates tracked in issues #1–#6. No percentage-based production certification.

This report supersedes the earlier 18-test audit as the current status. Historical device results remain in `ISSUES.md`; they do not certify the updated build. Scope includes reported microphone noise/repeated activation, outside-app invocation, persistent assistant conversation, tool safety, UI, model configuration, and documentation.

## Findings and repairs in source

| Finding | Current change | Evidence still needed |
| --- | --- | --- |
| Repeated Android recognizer restarts caused audible microphone cues and could re-trigger SOL | Replace wake recognition loop with offline Vosk; coordinate microphone ownership | Real spoken phrases, silence, false positives, battery/OEM behavior |
| Assistant disappeared after a successful tool command | Keep panel available until explicit close or Android lifecycle dismissal | Multiple spoken commands, Close SOL, Back/dismiss |
| Assistant and wake listener could compete for the microphone or hear TTS | Owner-specific pause/resume coordination; serialized listening and speech output | Invoke over foreground app, background app, and during/after TTS |
| Main chat replaced its previous turn and voice input only filled the editor | Bounded visible history, three-turn context, automatic voice submission | Follow-up reference and speech-to-action check |
| App lacked a running-task Stop control and robust approval cleanup | Cancel running coroutine; deny pending approval on cancellation/disposal | Cancel before execution and during a protected workflow |
| Long approval content and keyboard reduced usability | Scrollable approval content, keyboard insets, conversation scrolling | Small-screen/large-font device check |
| Current-facing docs overstated readiness and omitted added capabilities | Update capability, architecture, privacy, and release claims | Keep evidence table updated after final tests |

The API default is GPT-6 Astra with low reasoning effort. A live model availability request succeeded during this audit; this alone does not validate complete device workflows.

## Validation status for this revision

| Check | Status |
| --- | --- |
| MainActivity Kotlin compilation during development | Passed |
| Full debug build, unit suite, Android lint | Passed: 29 tests, 0 lint errors; non-blocking warnings remain |
| Installation of final combined APK | Passed on connected Nothing phone, Android 16 / API 36 |
| Bundled offline wake model and audio startup | Passed: archive extracted, native model/grammar loaded, foreground audio service started outside app |
| Fresh wake phrase outside app / no repeated popups | Pending physical-device test |
| Persistent assistant, spoken follow-up, explicit close | Pending physical-device test |
| Call/SMS approval and cancellation | Historical evidence; fresh regression pending |
| Native Open Spotify through Astra | Passed: model/tool/result round trip, Spotify confirmed foreground, result visible on return |
| Accessibility disabled recovery | Passed: Settings opened; observation reported ACCESSIBILITY_DISABLED and did not falsely claim a scroll |
| Accessibility observe, scroll, Back, Home | Fresh action regression blocked until user re-enables SOL screen control |
| Chrome tap and type | Open qualification case; do not label verified |
| Model availability and tool protocol | GPT-6 Astra live text response and structured echo function call both completed |
| Release credential guard | Generated release BuildConfig verified to contain an empty API key |

## Required regression pass

1. Leave SOL enabled at the launcher in silence; check for microphone beeps and unsolicited popups. Speak Hey SOL once and confirm one invocation.
2. Ask a question, hear the answer, ask a follow-up, and confirm the panel remains. Say Close SOL during listening and verify dismissal.
3. Run a native multi-step workflow; distinguish Android intent acceptance from visible completion.
4. Cancel protected call/SMS and tap/type requests; verify nothing executes. Approve only a reviewable test draft.
5. Stop a running workflow and dismiss an assistant with pending approval; verify no later tool executes.
6. Test missing app, denied permission, unavailable network, and unavailable screen with readable errors.
7. Observe, scroll, and re-observe a known app; qualify Chrome tap/type separately.

## Release limits

This is a technical alpha, not a production certification. Debug builds contain the local model key and must not ship publicly. Release builds omit it; a secured backend is still required. Vosk wake audio stays on-device, but Android command recognition may use the installed recognition provider, and model requests/screen context use OpenAI. TTS is final-answer speech, not full-duplex streaming audio.

The app has bounded in-memory text context, not durable conversation storage. Accessibility labels and visible observations cannot guarantee semantic safety across every app. Calls remain dialer flows and SMS remains draft preparation.

Production and deferred integration work is tracked in [GitHub issues](https://github.com/anandh0u/android-solappan/issues). No percentage in this report substitutes for device qualification.
