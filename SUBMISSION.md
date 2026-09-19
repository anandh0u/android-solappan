# Solappan — Android Agent Runtime

## Track

**Track 04 — Next-Gen Productivity & Automation**

## One-Line Pitch

Solappan turns Android into a controlled tool environment where an AI agent can plan and execute multi-step mobile workflows with transparent progress and human approval.

## Problem

AI models can reason about complex goals, but mobile assistants usually stop at conversation or isolated commands. Users still switch between apps, repeat context, find contacts, configure alarms, and prepare messages manually.

## Solution

The user describes an outcome once. The agent interprets the goal, selects allowlisted Android tools, validates parameters, executes native actions, reports structured results, and pauses for approval before consequential actions.

```text
USER GOAL
   ↓
UNDERSTAND + PLAN
   ↓
SELECT REGISTERED TOOLS
   ↓
VALIDATE + CHECK RISK
   ↓
EXECUTE ON ANDROID
   ↓
VERIFY RUNTIME RESULT
   ↓
COMPLETE OR RECOVER
```

## What Makes It Different

This is not “ChatGPT can open apps.” It is an agent runtime with a reusable boundary between model reasoning and device execution:

- The model decides what should happen.
- The runtime decides what is allowed and how it happens.
- Unknown tools and malformed arguments are rejected.
- Phone numbers remain inside the Android runtime.
- Calls and messages require in-app approval and final user action in the system app.

## Working Tools

- `open_app`
- `open_maps`
- `set_alarm`
- `find_contact`
- `call_contact`
- `prepare_sms`

## Demonstration

1. “Open Spotify.”
2. “Set an alarm for 7 AM tomorrow and navigate to GEC Thrissur.”
3. “Find Afnan, prepare a message saying I will reach 20 minutes late, and open Maps to GEC Thrissur.”

The third scenario demonstrates planning, multiple tools, contact lookup, privacy-preserving data handling, explicit approval, and cross-app execution.

## Reliability

- Eleven automated tests
- Repeated physical-device demo validation
- Offline, permission-denied, missing-app, missing-contact, cancellation, and malformed-response testing
- Bounded agent loop
- One safe retry for rate-limit/server failures
- No arbitrary model-generated code execution

![Solappan running on Android](docs/screenshots/home.png)

## Technology

Kotlin, Jetpack Compose, Android native APIs and intents, Android Contacts Provider, Android speech recognition, and the OpenAI Responses API with structured function calling.

## Future Direction

Add outcome observation, selective app integrations, calendar/email productivity tools, and carefully sandboxed accessibility assistance. The allowlisted tool boundary and approval model remain unchanged as capabilities grow.
