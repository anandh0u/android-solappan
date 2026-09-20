# SOL · Agent Runtime for Android

**Controlled Android agent runtime**

Give SOL a goal. It reasons through the task, chooses registered Android tools, asks for approval when needed, and reports the result.

> “Set an alarm for 7 AM, navigate to GEC Thrissur, and prepare a message to Afnan saying I’ll meet him there.”

Built with Codex, Kotlin, and Android native APIs. The model decides **what should happen**; the runtime decides **what is allowed and how it happens**.

[Watch the recorded demo / pitch video](https://drive.google.com/drive/folders/11loQKqyGb1A48FmHeaw9Y1IQ5X1SPlkI) · [Demo script](DEMO_SCRIPT.md) · [Submission](SUBMISSION.md) · [Audit evidence](AUDIT.md) · [Roadmap / issues](https://github.com/anandh0u/android-solappan/issues)

<p align="center">
  <video src="videos/sol-motion/renders/sol-ui-demo.mp4" controls muted playsinline width="360" poster="docs/screenshots/assistant-demo.png"></video>
</p>

<p align="center"><a href="https://drive.google.com/drive/folders/11loQKqyGb1A48FmHeaw9Y1IQ5X1SPlkI">Watch the recorded SOL demo / pitch video</a> · <a href="videos/sol-motion/renders/sol-ui-demo.mp4">Watch the short SOL UI clip</a> · <a href="videos/sol-motion/renders/sol-agent-runtime.mp4">Watch the runtime concept animation</a></p>

Current qualification: the signed-in phone completed a real Supabase gateway → registered app-discovery tool → model continuation round trip with no provider key embedded. The synthetic on-device message test also passed approval, recipient and duplicate-send checks. This is an MVP, not a production-certified release. Real messaging-app compatibility and delivery remain unverified; see the audit for exact evidence and remaining gates.

## Problem and solution

AI can reason well on mobile, but it is often trapped in a chat interface while people still switch manually among apps. SOL turns one high-level goal into a transparent Android workflow: the model selects from registered tools, while the runtime validates each action and asks for approval where consequences exist.

## One goal, multiple actions

```mermaid
flowchart LR
    A[Spoken or typed goal] --> B[Reason and select tools]
    B --> C{Validate and check risk}
    C -->|Protected action| D[User approval]
    C -->|Low risk| E[Execute on Android]
    D -->|Approved| E
    D -->|Declined| G[Report result]
    E --> F[Read result / observe visible UI]
    F -->|More steps| B
    F -->|Finished or blocked| G
    G --> H[Display and speak answer]
```

## Architecture

```mermaid
flowchart TB
    User[User: text, voice, gesture, tile or Hey SOL] --> Surface[Chat or Android assistant surface]
    Surface --> Coordinator[Agent runtime coordinator]
    Coordinator --> Controller[Bounded agent controller]
    Controller --> Registry[Registered tool registry]
    Controller <--> Gateway[Authenticated Supabase gateway]
    Gateway <--> Model[OpenAI Responses API]
    Registry --> Native[Android intents and native APIs]
    Registry --> Screen[Optional restricted Accessibility service]
    Native --> Result[Structured tool result]
    Screen --> Result
    Result --> Controller
    Controller --> Surface
```

There is one agent pipeline. Chat, the Android assistant panel, the Quick Settings tile and Hey SOL are entry points—not separate agents. The model chooses from schemas supplied by the registry; the Android runtime validates parameters, manages permissions and executes only known tools.

## Runtime sequence

```mermaid
sequenceDiagram
    participant U as User
    participant S as SOL surface
    participant A as Agent controller
    participant G as Supabase gateway
    participant M as OpenAI
    participant T as Tool registry
    participant D as Android device

    U->>S: Goal in text or speech
    S->>A: Submit goal
    A->>G: Authenticated request + tool schemas
    G->>M: Bounded Responses request
    M-->>A: Final text or registered tool call
    A->>T: Validate name, JSON and risk
    alt Protected action
        T-->>S: Approval request
        S-->>A: Approve or cancel
    end
    A->>D: Execute allowed Android action
    D-->>A: Structured result
    A->>G: Tool output / continuation
    G->>M: Continue reasoning
    M-->>S: Concise final result
```

## Security model

```mermaid
flowchart LR
    Model[Model output is untrusted] --> Allowlist{Registered tool?}
    Allowlist -->|No| Reject[Reject unknown tool]
    Allowlist -->|Yes| Schema{Valid parameters?}
    Schema -->|No| Invalid[Return structured error]
    Schema -->|Yes| Risk{Protected action?}
    Risk -->|No| Execute[Execute bounded tool]
    Risk -->|Yes| Approval[Require user approval]
    Approval -->|Denied| Cancel[Return cancelled]
    Approval -->|Approved| Execute
    Execute --> Verify[Return result / optional visible observation]
```

| Boundary | What SOL does |
| --- | --- |
| Tool authority | Rejects unknown names and extra/invalid arguments. It never runs model-generated code or shell commands. |
| Consequential actions | Calls, SMS drafts and message sending require a separate approval grant. Cancelling or dismissing the assistant denies a pending grant. |
| Demo access | A user-controlled 10-minute navigation grant can skip repeated approvals for ordinary taps and typing only. It expires across reboot and can be revoked. |
| Sensitive UI | Screen tools reject password fields and sensitive labels. Generic tap cannot approve, call, purchase or send. |
| External outcomes | Intent dispatch and a visible UI change are not treated as proof that a message delivered, music played or an alarm saved. |

## Gateway and account flow

```mermaid
flowchart LR
    Account[SOL account sign-in] --> Session[Keystore-encrypted session]
    Session --> Gateway[Supabase Edge Function]
    Gateway --> Verify[Verify user and account access]
    Verify --> Quota[Reserve quota and continuation ownership]
    Quota --> Request[Fixed model policy + registered tools]
    Request --> Response[Sanitized response to phone]
```

The phone build uses gateway mode and does not embed the provider key. The gateway verifies the signed-in user, applies account access and quota checks, and owns model credentials. Passwords are not stored by the app. Session data is encrypted with Android Keystore in no-backup storage.

## Features and screenshots

<p align="center">
  <img src="docs/screenshots/home.png" width="280" alt="SOL chat screen on the connected Android phone" />
  <img src="docs/screenshots/assistant-demo.png" width="280" alt="SOL assistant panel on the connected Android phone" />
</p>

- **Chat:** a focused SOL conversation, microphone, Send, Stop, and a compact Setup panel.
- **Demo access:** a visible, opt-in 10-minute grant skips repeated approvals for ordinary screen taps and typing. Tap the banner to revoke, or press Stop. Message sending and call/SMS flows still require approval; passwords, security actions and app locks remain protected. It is not unrestricted Android access.
- **System assistant:** invoke with the Android gesture or SOL Quick Settings tile. SOL stays open after a command; use **Close SOL**, or say “Close SOL” during an active listening turn.
- **Follow-ups:** three recent text turns provide bounded, in-memory context. Screenshots and approval grants are not retained as conversation memory.
- **Voice:** Android recognizes commands; Android TTS speaks final answers. After speech output, the assistant offers a follow-up listening turn. Silence leaves the panel available with **Talk again**. A 20-second watchdog prevents an indefinitely stuck recognizer; **Type in SOL** is always available as a fallback.
- **Optional wake phrase:** an offline Vosk listener with a bundled small English model listens for **Hey SOL** while its foreground service is enabled. Microphone ownership is coordinated with chat, assistant listening, and speech output.

Wake behavior is under fresh device validation. It is not an OEM low-power hotword implementation, and continuous use has a battery cost. The gesture and tile remain available.

## Sixteen registered device tools

| Native APIs and intents | Optional screen control |
| --- | --- |
| `list_apps`, `open_app`, `open_maps`, `set_alarm` | `observe_screen`, `scroll_screen` |
| `find_contact`, `call_contact`, `prepare_sms` | `tap_element`, `type_text` |
| `search_music`, `control_media` | `press_back`, `press_home` |
| | Experimental `send_message` (explicit approval) |

Calls open the dialer; `prepare_sms` opens drafts. Experimental `send_message` can press a visible English-labelled Send button after approval, checking the app, exact draft and a matching visible recipient above it. It does not prove recipient semantics or delivery across arbitrary apps; WhatsApp end-to-end qualification is still pending. Generic taps cannot send. Music uses Android's standard media search/play intent for a dynamically resolved installed app, with observed UI fallback when unsupported. No playback guarantee is made. Screen control requires Accessibility consent and an unlocked phone.

## Tool capability matrix

| Tool family | Examples | Confirmation | Verification boundary |
| --- | --- | --- | --- |
| App and navigation intents | `list_apps`, `open_app`, `open_maps`, `set_alarm` | No | Android accepting an intent is not outcome proof. |
| Contacts and communication | `find_contact`, `call_contact`, `prepare_sms` | Call/SMS yes | Calls open the dialer; SMS opens a draft. |
| Media | `search_music`, `control_media` | No | Search intent/media-key dispatch does not prove playback. |
| Screen observation | `observe_screen`, `scroll_screen`, `press_back`, `press_home` | No | Requires enabled Accessibility and unlocked screen. |
| Restricted screen actions | `tap_element`, `type_text` | Yes, or active demo navigation consent | Fresh observed targets only; sensitive targets are blocked. |
| Experimental direct send | `send_message` | Always yes | One-shot action; delivery and arbitrary-app recipient semantics remain unverified. |

## Voice and assistant lifecycle

```mermaid
stateDiagram-v2
    [*] --> Ready
    Ready --> Listening: Gesture, tile, wake or Talk again
    Listening --> Thinking: Transcript received
    Listening --> Ready: Silence / timeout / error
    Thinking --> AwaitingApproval: Protected tool requested
    AwaitingApproval --> Executing: User approves
    AwaitingApproval --> Ready: User cancels or assistant closes
    Thinking --> Executing: Low-risk tool requested
    Executing --> Speaking: Final response
    Speaking --> Ready: Follow-up or Talk again
```

Android speech recognition handles commands and Android TTS speaks final answers. Vosk provides the optional foreground-service Hey SOL listener. The wake listener pauses while the assistant or TTS owns the microphone, avoiding feedback and competing recorders. Full-duplex realtime voice and barge-in are not implemented.

## Try a workflow

1. “Open Spotify.”
2. “Search Spotify for Starboy.”
3. “Set an alarm for 7 AM and navigate to GEC Thrissur.”
4. “Prepare a message to Afnan saying I’ll reach 20 minutes late.”
5. Optional screen-control demo: “Open Chrome, observe the screen, tap the address bar, type Solappan agent demo, then observe again. Do not submit.”

The fifth workflow remains a device qualification case; see the [audit](AUDIT.md) before presenting it as a verified demo.

## Run locally

Use Android Studio, JDK 17, Android SDK 35, and an Android 8.0+ device. Physical-device testing is recommended.

Add private configuration to the Git-ignored `local.properties`:

```properties
OPENAI_API_KEY=your_key_here
OPENAI_MODEL=gpt-6-astra
USE_GATEWAY=false
```

The current default is **GPT-6 Astra**, using low reasoning effort. Model availability was checked with a live API request during this audit. The wake engine uses **Vosk Android 0.3.75** with a bundled small English model; command reasoning still requires internet access.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

On the phone, open **Setup**, grant microphone permission, choose SOL as the default assistant, and grant contacts only for contact workflows. Enable screen control only for accessibility tools. Enable Hey SOL explicitly; its persistent notification provides a Stop action.

Release builds omit the local key and require the authenticated Supabase gateway. The gateway and SQL migration are deployed; live authentication, access controls, quota enforcement and conversation-isolation smoke tests passed. The current phone build uses gateway mode and also excludes the provider key. Create and verify a SOL account in Setup, obtain owner approval and sign in. CLI/dashboard login is not an app account. Device sign-in/refresh and full agent workflows still require qualification. See [gateway setup and privacy boundaries](docs/GATEWAY.md). **Never distribute a developer-mode APK containing a provider key.**

## Runtime boundaries

Only registered tools execute. The registry rejects unknown tools and invalid arguments. Protected actions require explicit user approval; screen content cannot grant it. No model-generated code or shell commands run automatically.

Contact lookup returns IDs and display names, keeping raw numbers out of its tool result. Explicit screen context can still contain private visible information. Password filtering is not comprehensive screenshot redaction. Local context clearing does not guarantee provider-side deletion.

Intent acceptance means Android received a request. It does not prove that an alarm was saved, navigation began, or music played. Screen observation verifies visible state, not real-world outcomes.

## Codex and OpenAI usage

Codex was used to build, test, debug and document the Android runtime. SOL uses the OpenAI Responses API through an authenticated Supabase Edge Function; the model receives registered tool schemas and the Android app independently validates every requested action. Provider credentials are held by the gateway, never committed to the repository or packaged in the release app.

## Live / hosted project

SOL is a native Android application rather than a hosted website. The public recorded device demo is available in [Google Drive](https://drive.google.com/drive/folders/11loQKqyGb1A48FmHeaw9Y1IQ5X1SPlkI); build and install instructions are above.

## Release status

**Hackathon prototype / technical alpha.** Android has 42 passing unit tests and the gateway has 10 passing local policy/auth tests; debug/instrumentation assembly and lint pass. Hosted gateway smoke tests and a signed-in phone tool round trip passed. The user confirmed unlocked Hey SOL invocation. Synthetic on-device sending passed without contacting anyone; real messaging-app delivery and broader release qualification remain open. [AUDIT.md](AUDIT.md) records the evidence.

Production work includes qualifying signed-in phone workflows, OEM/battery behavior, accessibility action assurance, realtime audio, lifecycle persistence, release/privacy qualification, and selected integrations. These remain tracked in [GitHub issues](https://github.com/anandh0u/android-solappan/issues). CI checks Android compilation/tests/lint, instrumentation-test compilation, release key exclusion and gateway policy/authentication tests. Passing CI is not proof of device or public-release readiness.

## Evidence at a glance

| Evidence | Current result |
| --- | --- |
| Android quality gate | 42 unit tests, debug and instrumentation APK assembly, and lint pass. |
| Gateway quality gate | 10 policy/authentication tests plus hosted authentication, quota and continuation smoke tests. |
| Phone evidence | Signed-in gateway tool round trip, synthetic approved-send control path, unlocked Hey SOL invocation and final APK launch. |
| Demo material | Real phone UI screenshots, a 7-second UI demo clip and an 8-second architecture concept animation. |
| Open limits | Real WhatsApp delivery, arbitrary-app semantic safety, OEM wake/battery behavior, full-duplex audio and release/privacy qualification. |

## Explore the repository

[Architecture](PROJECT.md) · [Product requirements](PRD.md) · [Decisions](DECISIONS.md) · [Validation history](ISSUES.md) · [Judge Q&A](JUDGES_QA.md)

[Third-party notices](THIRD_PARTY_NOTICES.md) · [Official model migration guidance](https://developers.openai.com/api/docs/guides/latest-model/gpt-6-astra)
