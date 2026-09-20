# Product Requirements Document

> Core hackathon baseline. Subsequent authorized upgrades add optional assistant voice, offline wake and restricted accessibility. The current assistant remains open until user dismissal (superseding earlier upgrade examples that auto-dismissed). See README.md, AUDIT.md and the GitHub backlog linked from ISSUES.md for current capability and verification boundaries. Production voice/full-device automation remains a separate release effort.

## 1. Product Name

Temporary name:

**Solappan — Android Agent Runtime**

Category: **Android agent runtime**.

## 2. Problem

Modern AI models are capable of sophisticated reasoning but mobile interaction is often restricted to chat interfaces or predefined assistant integrations.

Users cannot easily give a general goal such as:

> "Get me ready for college."

and have an agent reason about the required steps and use multiple Android capabilities to achieve that goal.

## 3. Product Vision

Create an Android agent runtime that turns natural-language goals into safe, structured actions on a mobile device.

The model should not receive unrestricted Android control.

Instead, Android capabilities are exposed as explicit registered tools.

## 3.1 Product Positioning

- **Agent:** accepts high-level goals rather than isolated commands.
- **Automation:** completes multi-step workflows across Android.
- **Tools:** exposes Android functions through a controlled registry.
- **Productivity:** reduces repetitive app switching and manual actions.

The product is an agent runtime, not a general chatbot and not unrestricted device automation.

## 4. Primary User Flow

User enters a natural-language request.

↓

Agent interprets goal.

↓

Agent generates a plan.

↓

Agent selects registered tools.

↓

Runtime validates requested tool.

↓

Runtime checks risk level.

↓

If necessary, user confirmation is requested.

↓

Android executes action.

↓

Result is returned to agent.

↓

Agent continues or completes task.

## 5. MVP Requirements

### P0 — Mandatory

The application must:

- launch without crashing
- accept text input
- communicate with an OpenAI model
- support model tool calling
- maintain a tool registry
- execute more than one tool in a task
- display agent progress
- handle tool failures
- require confirmation for consequential actions

Required tools:

- `open_app`
- `open_maps`
- `set_alarm`
- `find_contact`
- `call_contact`

Preferred additional tool:

- `prepare_sms`

## 6. Agent States

The UI and runtime should support:

- IDLE
- THINKING
- PLANNING
- EXECUTING
- WAITING_FOR_CONFIRMATION
- VERIFYING
- COMPLETED
- FAILED
- CANCELLED

## 7. Risk Levels

### LOW

Actions that normally do not create significant consequences.

Examples:

- opening an app
- opening Maps
- searching contacts

### MEDIUM

Actions with external effects.

Examples:

- dialing/calling someone
- preparing or sending messages
- changing device settings

### HIGH

Not required for MVP.

Examples:

- payments
- purchases
- deleting data
- account changes
- security configuration

High-risk functionality should not be implemented during the core hackathon build.

## 8. Confirmation Rules

User confirmation is required before consequential actions.

Examples:

- calling
- sending messages
- destructive actions

The agent must show the proposed action before execution.

## 9. Non-Goals

During the core build, DO NOT attempt to build:

- a production voice assistant
- complete Android control
- support for every app
- payment automation
- autonomous purchases
- autonomous account modification
- unrestricted shell execution
- unrestricted generated code execution
- a complete backend platform
- an entire Accessibility automation framework
- local LLM inference

## 10. Success Criteria

The MVP is successful when three demo scenarios work repeatedly without manual developer intervention.

### Demo 1

"Open Spotify."

### Demo 2

"Set an alarm for 7 AM tomorrow and navigate to GEC Thrissur."

### Demo 3

"Find Afnan, prepare a message saying I'll reach 20 minutes late, and open Maps to GEC Thrissur."

## 11. Hackathon Principle

Do not measure success by feature count.

Measure success by:

- reliability
- clarity of demo
- agent behaviour
- execution transparency
- user control
