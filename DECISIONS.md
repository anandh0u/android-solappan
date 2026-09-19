# Architecture Decision Log

## ADR-001 — Native Android

Status: Accepted

The project will be implemented as a native Android application.

## ADR-002 — Kotlin

Status: Accepted

Kotlin is the main development language.

Do not migrate languages during the hackathon.

## ADR-003 — Jetpack Compose

Status: Accepted

Jetpack Compose will be used for the primary interface.

Do not replace Compose with XML layouts unless an Android integration absolutely requires it.

## ADR-004 — Tool-Based Agent Architecture

Status: Accepted

The language model never receives unrestricted device control.

Android capabilities are exposed through explicit registered tools.

Flow:

Model

↓

Tool request

↓

Tool Registry

↓

Validation

↓

Execution

## ADR-005 — Native First

Status: Accepted

Implementation priority:

1. Native Android API
2. Android Intent
3. Official application/API integration
4. Accessibility/UI automation

## ADR-006 — Accessibility Is Optional

Status: Accepted

AccessibilityService must not be required for the core MVP.

It is a stretch capability.

## ADR-007 — Human Approval

Status: Accepted

Consequential actions require explicit user confirmation.

## ADR-008 — Single Main Agent UI

Status: Accepted

Do not build a complex multi-screen application.

The hackathon demo should primarily use one polished agent interface.

## ADR-009 — Reliability Before Feature Count

Status: Accepted

Do not add tools simply to increase the feature count.

Core tools must remain stable.

## ADR-010 — No Automatic Generated-Code Execution

Status: Accepted

Even if Codex-generated capability creation is implemented later, generated code must not automatically receive unrestricted execution privileges.

## ADR-011 — Keep Contact Numbers Inside the Android Runtime

Status: Accepted

Contact search results sent back to the model contain only an internal contact ID and display name. Raw phone numbers remain inside the Android runtime and are resolved only when constructing a dialer or SMS intent.

Calls use `ACTION_DIAL` rather than direct calling, and messages use an `ACTION_SENDTO` draft. The system application retains the final Call or Send action as defense in depth; the dedicated in-app approval flow remains a Session 4 requirement.

## ADR-012 — Defense-in-Depth Confirmation

Status: Accepted

Consequential tools require two independent runtime conditions: the user checks a review control and taps the approval action, then `ToolRegistry` receives an explicit confirmation grant from `AgentController`.

Calling a protected tool through the registry without that grant returns `CONFIRMATION_REQUIRED` and never reaches the Android intent.
