# SOL — Agent Runtime for Android

SOL turns Android into a controlled tool environment for AI. From one spoken or typed goal, the model can plan a workflow, select registered tools, execute Android actions, request approval for consequential steps, inspect visible results, and report what actually happened.

Built for **Track 04 — Next-Gen Productivity & Automation**.

> This is not “a chatbot that can open apps.” It is an agent runtime that exposes safe, composable Android capabilities to a reasoning model.

## Experience

Invoke SOL with the Android assistant gesture, the SOL Quick Settings tile, or the optional **Hey SOL** listener. Speak naturally:

> “I’m leaving for college. Navigate to GEC Thrissur, set an alarm for 7 tomorrow, and prepare a message to Afnan saying I’ll meet him there.”

The same agent pipeline handles text and voice:

```text
Goal → OpenAI reasoning → registered tool calls → validation/approval
     → Android execution → observation → final spoken response
```

The app screen is intentionally minimal: SOL identity, conversation, microphone/send controls, wake toggle, and a collapsed Setup panel. The Android assistant uses a compact temporary surface rather than opening the normal app.

## Current capabilities

Native tools:

- `open_app`
- `open_maps`
- `set_alarm`
- `find_contact`
- `call_contact` — opens the dialer after approval; the user presses Call
- `prepare_sms` — opens a draft after approval; the user presses Send
- `search_music` — opens Spotify search results without claiming autoplay
- `control_media` — play, pause, next, or previous through Android media controls

Optional accessibility fallback:

- `observe_screen`
- `tap_element`
- `type_text`
- `scroll_screen`
- `press_back`
- `press_home`

Voice features:

- Android assistant role and temporary assistant UI
- Android speech-to-text feeding the same `AgentController`
- Spoken final responses through Android text-to-speech
- Optional foreground **Hey SOL / Hello SOL / Okay SOL** listener
- Quick Settings tile and normal assistant gesture remain reliable alternatives

## Setup on the phone

1. Open SOL and tap **Setup**.
2. Confirm Model, Microphone, and Contacts are enabled.
3. Select SOL as the default digital assistant.
4. Enable optional screen control only if demonstrating accessibility automation.
5. Tap **Hey SOL: Off** to enable the wake listener. Android shows a persistent low-priority notification while the microphone service is active.

The wake listener uses Android’s speech recognizer and does **not** continuously upload microphone audio to OpenAI. It is experimental, consumes additional battery, and may be affected by OEM background restrictions. Turn it off from the SOL header or notification action when not needed.

## Safety model

- Only registered tools execute.
- Unknown tools and malformed/extra parameters are rejected.
- The model cannot approve its own protected action.
- Call, SMS, tap, and text-entry flows retain explicit user control.
- Accessibility never replaces an available native API or intent.
- Password fields, SOL’s own UI, ambiguous targets, and sensitive labels such as Send, Call, Pay, Delete, Allow, or Approve are blocked from generic accessibility actions.
- Screen content is untrusted data, not instructions or authorization.
- No arbitrary model-generated shell commands or code are executed.
- Contact phone numbers stay inside the Android runtime.

## Architecture

```text
Android assistant / Hey SOL / app text input
                    ↓
          AgentRuntimeCoordinator
                    ↓
             AgentController
                    ↓
          OpenAI Responses API
                    ↓
              ToolRegistry
         ┌──────────┼──────────┐
    Android APIs   Intents   Optional accessibility
         └──────────┼──────────┘
                    ↓
            structured results
```

There is one reasoning and execution pipeline. Voice, wake invocation, the temporary assistant, and the Compose screen do not implement separate command parsers.

## Local development

Requirements:

- Android Studio / Android SDK 35
- JDK 17
- Android 8.0+ device; a physical phone is recommended

Create or update the Git-ignored `local.properties`:

```properties
OPENAI_API_KEY=your_key_here
OPENAI_MODEL=gpt-5-mini
```

Build and verify:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

The direct API key is acceptable only for this local hackathon prototype. A public build must use a secured backend/proxy, authentication, rate limiting, and proper privacy/retention controls.

## Demo prompts

1. `Open Spotify.`
2. `Search Spotify for Starboy.`
3. `Set an alarm for 7 AM tomorrow and navigate to GEC Thrissur.`
4. `Text Afnan saying I’ll reach 20 minutes late.`
5. `Open Chrome, observe the screen, tap the address bar, type Solappan agent demo, then observe again. Do not submit.`

## Honest limitations

- Intent acceptance proves Android received a request, not that navigation started or an alarm was ultimately saved.
- Spotify search routing is verified; automatic selection and playback of an exact result is not guaranteed.
- Accessibility behavior varies by app, Android version, and manufacturer and remains experimental.
- The wake phrase is an opt-in foreground service, not a production-grade low-power hotword engine.
- Text-to-speech begins after the final model response; this is spoken response output, not full-duplex streaming audio.
- Screen context sent for explicit screen questions may contain sensitive visible information. Local clearing does not delete provider-side API records.
- There is no production backend, telemetry, account system, Play Store policy package, or release hardening.

## Project documents

- [Product requirements](PRD.md)
- [Technical context](PROJECT.md)
- [Build plan](BUILD_PLAN.md)
- [Architecture decisions](DECISIONS.md)
- [Issue and validation record](ISSUES.md)
- [Submission pitch](SUBMISSION.md)
- [Demo script](DEMO_SCRIPT.md)
