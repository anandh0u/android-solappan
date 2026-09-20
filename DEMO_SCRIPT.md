# Solappan Demo Script

## Before Presenting

- Confirm internet connectivity.
- Grant Contacts permission.
- Confirm Spotify, Maps, Clock, and Messages are installed.
- Confirm the contact “Afnan” exists.
- Reset Solappan to the Idle state.
- Keep the backup APK and recording available.
- Read AUDIT.md before choosing optional demonstrations. Hey SOL phrase recognition and generic Chrome tap/type still need human/device qualification.
- For voice: select SOL as default assistant, enable microphone, optionally enable Hey SOL and leave the app. Say the wake phrase separately, then the request. The assistant remains open; use Close SOL when finished.
- Use the installed gateway-mode build. The current debug and release configurations omit the provider key; never distribute a developer-mode build if one is configured with a direct provider key.

## 90-Second Demo

### 0:00–0:15 — Problem

“Powerful AI is still trapped inside chat on mobile. To finish a real workflow, users manually jump between apps.”

### 0:15–0:30 — Architecture

“Solappan turns Android capabilities into registered tools. The model plans the workflow, but the Android runtime validates every tool, parameter, risk level, and approval.”

### 0:30–1:10 — Main Workflow

Run:

> Find Afnan, prepare a message saying I will reach 20 minutes late, and open Maps to GEC Thrissur.

Narrate the visible timeline:

1. Contact lookup completes without exposing the number to the model.
2. Maps opens the destination.
3. Solappan pauses before preparing the message.
4. Check “I reviewed this action,” then tap “Approve action.”
5. Messages opens with a draft; do not send it.

### 1:10–1:25 — Safety

“The model cannot invent executable capabilities. Unknown tools fail safely. Calls and messages pause for approval and still require the final system-app action.”

### 1:25–1:30 — Close

“This is not another chatbot. It is an agent runtime that turns Android into a safe tool environment for AI.”

## Backup Demos

- `Open Spotify.`
- `Set an alarm for 7 AM tomorrow and navigate to GEC Thrissur.`

## Recovery Lines

- Network issue: “The runtime surfaces a structured failure rather than crashing; I’ll use the recorded run.”
- Permission denied: “Permissions are optional and recoverable from the main screen.”
- Contact ambiguity: “The runtime refuses to guess between contacts.”
