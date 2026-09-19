# Android Agent Runtime

An experimental Android AI agent built for a 12-hour Codex hackathon.

The project gives an AI reasoning model the ability to understand a user's natural-language goal, create a plan, select registered Android tools, execute actions on the device, verify results, and keep the user in control of consequential actions.

## Core Idea

Traditional mobile AI assistants mainly answer questions or execute predefined commands.

This project explores a more general agent architecture:

**User Goal → Reasoning → Planning → Tool Selection → Android Execution → Verification**

Example:

User:

> "I'm leaving for college. Navigate to GEC Thrissur, set an alarm for 7 AM tomorrow, and prepare a message to Afnan saying I'll meet him there."

Agent:

1. Understand the goal.
2. Generate a plan.
3. Invoke `open_maps()`.
4. Invoke `set_alarm()`.
5. Find the requested contact.
6. Prepare a message.
7. Ask for user approval.
8. Execute the approved action.
9. Report completion.

## Hackathon Objective

Build a reliable working MVP within 12 hours.

The goal is NOT to build a production Android assistant.

The goal is to demonstrate:

- natural-language task understanding
- LLM tool calling
- Android device actions
- multi-step execution
- human approval
- agent progress UI
- basic verification

## Core MVP

The MVP should support:

- Text input
- OpenAI model integration
- Tool/function calling
- Android tool registry
- Open application
- Find contact
- Call/dial contact
- Set alarm
- Open Maps/navigation
- Prepare SMS/message
- Agent execution trace
- Confirmation system
- Error handling

## Stretch Features

Only after the core MVP is stable:

- Voice input
- Screenshot understanding
- AccessibilityService
- Tap/type/swipe automation
- UI verification
- Additional Android tools
- Codex-generated capabilities
- Local/on-device models

## Technology

- Kotlin
- Android
- Jetpack Compose
- Coroutines
- Android Intents
- Android Contacts API
- OpenAI API
- Structured tool calling

## Build Philosophy

Reliability is more important than number of features.

Five tools that work every time are better than thirty tools that occasionally work.