# 12-Hour Build Plan

> Historical implementation plan. Later user-authorized voice/accessibility upgrades and the 2026-09-20 audit extend this scope. Current behavior and evidence are in README.md and AUDIT.md; remaining production and stretch work is linked at the top of ISSUES.md. Completed boxes below describe earlier milestones, not release certification.

## Locked Track and MVP

Track: **04 — Next-Gen Productivity & Automation**.

The core MVP is one text-driven agent screen, an OpenAI reasoning loop, a strict Android tool registry, multi-tool execution, visible progress, confirmation for consequential actions, structured failures, and five required Android tools: `open_app`, `open_maps`, `set_alarm`, `find_contact`, and `call_contact`. `prepare_sms` remains preferred after the required path is stable.

Supabase, voice, accessibility automation, arbitrary generated-code execution, and additional tools are outside the locked MVP.

## Global Rule

ONLY execute the current session.

Do not implement future sessions unless explicitly instructed.

Stretch goals are forbidden until the core MVP is stable.

---

# SESSION 1 — FOUNDATION

## Time

Hour 0–2

## Objective

At the end of this session:

User enters text.

↓

Request reaches OpenAI.

↓

Model response appears in Android UI.

## Tasks

### 1. Create Android Application

Use:

- Kotlin
- Jetpack Compose
- minimum reasonable SDK configuration

### 2. Create Basic UI

Include:

- title
- text field
- send button
- response area
- loading state
- error display

Do not spend significant time styling.

### 3. Model/API Integration

Implement API communication.

Requirements:

- configurable API key
- asynchronous request
- timeout handling
- error handling
- no UI-thread network operations

### 4. Logging

Add development logging for:

- request started
- request completed
- API failure

Do not log secrets.

## Acceptance Criteria

- [x] Project compiles
- [x] APK launches
- [x] User can type
- [x] User can submit
- [x] API request succeeds
- [x] Model response displays
- [x] Network error does not crash app
- [x] API key is not committed

## Stop Condition

When all acceptance criteria pass, stop Session 1.

Do not implement Android tools yet.

---

# SESSION 2 — AGENT CORE

## Time

Hour 2–4

## Objective

Turn the chatbot into a tool-using agent.

Expected flow:

User

↓

Model

↓

Tool Call

↓

Tool Registry

↓

Tool Result

↓

Model

↓

User

## Tasks

### 1. Create Tool Interface

Every tool requires:

- name
- description
- parameters
- risk level
- confirmation requirement
- execution method

### 2. Create Tool Registry

Responsibilities:

- register tools
- list available tools
- find tool by name
- reject unknown tools

### 3. Create Mock Tool

Implement:

`echo(message)`

Purpose:

Test function calling without Android complexity.

### 4. Agent Loop

AgentController must:

1. send user request
2. receive model output
3. detect tool call
4. validate tool
5. execute tool
6. return tool result
7. continue model response
8. stop when task is complete

### 5. Multi-Tool Support

The architecture must allow multiple sequential tool calls.

## Test Prompt

"Use the echo tool to say Hello Android."

Expected:

Model actually invokes `echo`.

It should NOT merely output that it would invoke the tool.

## Acceptance Criteria

- [x] Tool schema reaches model
- [x] Model calls mock tool
- [x] Tool executes
- [x] Tool result is returned
- [x] Unknown tool fails safely
- [x] Invalid parameters fail safely
- [x] Multiple tool calls are possible
- [x] Agent loop terminates correctly

## Stop Condition

Do not start Android tools until the generic tool mechanism works.

---

# SESSION 3 — ANDROID ACTIONS

## Time

Hour 4–6

## Objective

Give the agent real Android capabilities.

## Required Tools

### open_app

Input:

`appName`

Expected:

Launch installed app.

Failure:

Return app-not-found result.

### open_maps

Input:

`destination`

Expected:

Open Maps/navigation with destination.

### set_alarm

Input:

- hour
- minute
- label

Expected:

Create/open alarm flow.

### find_contact

Input:

`query`

Expected:

Return contact candidates.

### call_contact

Input:

contact identifier.

Expected:

Resolve number and initiate safe calling/dialing flow.

Requires confirmation:

YES.

## Optional

### prepare_sms

Input:

- recipient
- message

Expected:

Open SMS composer containing prepared message.

Prefer user-controlled sending.

## Permission Handling

Handle:

- contact permission
- phone permission if necessary

Never assume permission exists.

## Acceptance Criteria

- [x] Agent can open an installed app
- [x] Agent can open Maps
- [x] Agent can configure alarm flow
- [x] Agent can search contacts
- [x] Agent can prepare call flow
- [x] Missing permission produces understandable UI
- [x] Missing app does not crash
- [x] Missing contact does not crash

## Required End-to-End Test

Prompt:

"Set an alarm for 7 AM tomorrow and open Maps to Government Engineering College Thrissur."

Both actions must execute correctly.

## Critical Milestone

At the end of Session 3, the application MUST already be demoable.

Do not sacrifice this milestone for stretch functionality.

---

# SESSION 4 — AGENT UX AND SAFETY

## Time

Hour 6–8

## Objective

Make the system clearly look and behave like an agent.

## Agent States

Implement visible states:

- IDLE
- THINKING
- PLANNING
- EXECUTING
- WAITING_FOR_CONFIRMATION
- VERIFYING
- COMPLETED
- FAILED

## Execution Timeline

Example:

Goal:

"Get me ready for college."

Plan:

✓ Destination understood

✓ Maps opened

● Setting alarm

○ Preparing message

## Confirmation UI

For protected actions display:

Action:

Call Afnan

Risk:

This will place a phone call.

Buttons:

Cancel

Approve

Agent must not execute until approved.

## Error UI

Example:

`Contact "Afnan" could not be uniquely identified.`

Give the user useful feedback.

Do not show raw stack traces.

## Acceptance Criteria

- [x] Current agent state visible
- [x] Tool progress visible
- [x] Completed actions marked
- [x] Failed actions clearly marked
- [x] User approval works
- [x] User cancellation works
- [x] Agent can return to idle state
- [x] Final result is visible

---

# SESSION 5 — STABILIZATION AND DEMO

## Time

Hour 8–12

## Phase A — Hours 8–10

NO new major functionality.

Focus only on reliability.

## Demo Scenario 1

"Open Spotify."

Expected:

Correct application launches.

## Demo Scenario 2

"Set an alarm for 7 AM tomorrow and navigate to GEC Thrissur."

Expected:

Agent identifies two actions.

Alarm flow works.

Maps opens correct destination.

## Demo Scenario 3

"Find Afnan, prepare a message saying I'll reach 20 minutes late, and open Maps to GEC Thrissur."

Expected:

Agent:

1. resolves contact
2. prepares message
3. requests approval where necessary
4. opens Maps
5. shows completion state

## Reliability Testing

Run every demonstration repeatedly.

Test:

- normal execution
- no internet
- permission denied
- wrong contact
- missing app
- malformed model response
- repeated command
- cancellation

## Demo Backup

Prepare backup options:

- screen recording
- screenshots
- known stable prompts

A live demo remains preferred.

---

# STRETCH PHASE

Only begin if:

- all P0 features work
- demo scenarios work repeatedly
- build is stable
- APK installs correctly

## Stretch Priority 1 — Voice

Add speech-to-text input.

Keep text input available as fallback.

Expected:

Voice

↓

Speech-to-text

↓

Same AgentController

Do NOT build a separate voice architecture.

## Stretch Priority 2 — Screen Understanding

Capture or inspect current screen where technically appropriate.

Send visual context to a vision-capable model.

Possible use:

User:

"What is on my screen?"

Agent:

Analyzes screenshot.

## Stretch Priority 3 — Verification

After an action:

Observe result.

↓

Determine whether expected state occurred.

↓

If success:

continue.

If failure:

retry or report failure.

Concept:

PLAN

↓

ACT

↓

OBSERVE

↓

VERIFY

↓

CONTINUE / RECOVER

## Stretch Priority 4 — Accessibility

Potential actions:

- tap
- type
- swipe
- back
- scroll

Accessibility must remain experimental.

Do not let it destabilize the core application.

Prefer deterministic native actions whenever possible.

## Stretch Priority 5 — More Tools

Possible tools:

- read notifications
- create calendar event
- camera
- flashlight
- Bluetooth settings
- Wi-Fi settings
- volume
- browser search
- share content

Implement only if demo value is clear.

## Stretch Priority 6 — Codex Self-Extension

Experimental concept:

Agent receives task.

↓

Required capability does not exist.

↓

Runtime reports missing capability.

↓

Codex generates implementation proposal.

↓

Tool is reviewed/tested.

↓

Capability becomes available.

This is NOT required for the MVP.

Never allow unrestricted generated code execution.

---

# SECURITY

## Rule 1

The model is untrusted input.

Model output must be validated.

## Rule 2

Only registered tools may execute.

## Rule 3

Validate tool arguments.

## Rule 4

Require explicit approval for consequential actions.

## Rule 5

Do not store secrets in Git.

## Rule 6

Never display API secrets in logs.

## Rule 7

Do not implement payments or destructive operations during the MVP.

---

# TESTING STRATEGY

For every new tool test:

### Happy Path

Valid request and permissions.

### Invalid Input

Missing or malformed argument.

### Permission Failure

Required permission rejected.

### External Failure

Application/contact/service unavailable.

### Agent Failure

Model requests wrong or nonexistent tool.

### Recovery

System returns structured failure without crashing.

---

# DEMO STORY

## Opening

AI models have become extremely capable at reasoning, coding and understanding information.

But on mobile devices, those models are often trapped behind a conversation interface.

This project explores what happens when we give a reasoning model a controlled set of tools to act on Android.

## Demonstration

User gives one natural-language goal.

Agent:

1. understands it
2. creates a plan
3. selects Android capabilities
4. executes them
5. requests permission when necessary
6. verifies progress
7. reports completion

## Core Message

This is not another chatbot.

This is an experimental agent runtime for Android.

## Future Vision

Any capable reasoning model could connect to the runtime.

Android capabilities become tools.

Additional device, IoT, application or external-service integrations can be added without redesigning the reasoning layer.

---

# FINAL HACKATHON RULE

When deciding between:

A new feature

and

making an existing demo reliable,

always choose reliability.

A small agent that works is better than an ambitious agent that crashes.
