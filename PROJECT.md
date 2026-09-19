# Project Technical Context

## Purpose

This repository contains an experimental Android agent runtime built during a 12-hour hackathon.

The system allows an AI model to translate natural-language goals into structured Android tool calls.

The model does not directly control Android.

All actions pass through a controlled tool registry.

## Product Positioning

This project is locked to **Track 04 — Next-Gen Productivity & Automation**.

Its core claim is: **we built an agent runtime that turns Android into a tool environment for AI.** The model decides what should happen; the Android runtime validates what is allowed and determines how it happens.

## High-Level Architecture

User through Compose, Android assistant, Quick Settings, or optional Hey SOL

↓

Compose UI

↓

AgentController

↓

OpenAI Model

↓

Structured Tool Call

↓

ToolRegistry

↓

Risk / Permission Check

↓

Android Tool

↓

Android API / Intent

↓

Tool Result

↓

AgentController

↓

Model

↓

UI Result

## Main Components

### AgentController

Responsible for:

- sending requests to model
- maintaining conversation/task state
- receiving tool calls
- executing tools
- returning tool results
- continuing agent loop
- handling completion/failure

### ToolRegistry

Stores all available tools.

Responsibilities:

- register tools
- retrieve tool by name
- reject unknown tools
- expose tool schemas to model

### AndroidTool

Every tool should follow a common conceptual structure:

- name
- description
- parameters
- risk level
- confirmation requirement
- execute
- optional verify

## Tool Contract

Each tool should contain:

`name`

Unique tool identifier.

`description`

Clear explanation for the model.

`parameters`

Structured arguments.

`riskLevel`

LOW, MEDIUM, or HIGH.

`requiresConfirmation`

Boolean.

`execute()`

Runs Android functionality.

`verify()`

Optional validation of result.

## Core Tools

### open_app

Input:

- appName

Behaviour:

- resolve package
- launch installed application
- gracefully fail if not installed

Risk:

LOW

### open_maps

Input:

- destination

Behaviour:

- construct geo/navigation intent
- open mapping application

Risk:

LOW

### set_alarm

Input:

- hour
- minute
- label

Behaviour:

- use Android alarm intent/API

Risk:

LOW

### find_contact

Input:

- query

Behaviour:

- search Android contacts
- return matching contacts

Risk:

LOW

### call_contact

Input:

- contact reference

Behaviour:

- resolve phone number
- prepare dial/call action
- request confirmation when necessary

Risk:

MEDIUM

### prepare_sms

Input:

- recipient
- message

Behaviour:

- resolve recipient
- open SMS/message composer
- insert message
- allow user approval before sending

Risk:

MEDIUM

### control_media

Input:

- action: `play`, `pause`, `next`, or `previous`

Behaviour:

- dispatch a native Android media command to the active media session
- reject unsupported actions

Risk:

LOW

### search_music

Opens Spotify search results for a validated query. It does not claim that playback started.

Risk: LOW

### Optional accessibility tools

`observe_screen`, `tap_element`, `type_text`, `scroll_screen`, `press_back`, and `press_home` provide a restricted fallback when native Android APIs are unavailable. Tap and type require approval. Sensitive, password, ambiguous, hidden, disabled, stale, and SOL-owned targets are rejected.

## Security Requirements

Never hardcode:

- OpenAI API keys
- secrets
- tokens
- private phone numbers
- credentials

Do not commit secrets to Git.

For the hackathon prototype, API configuration may be stored locally.

For any public or production deployment, move sensitive API access behind a backend.

## Error Handling

Every tool must return a structured result.

Example result fields:

- success
- message
- data
- errorCode

Never crash because a tool fails.

Example failures:

- contact not found
- application not installed
- permission denied
- model produced malformed parameters
- Android Intent unavailable

Failures must be shown to the agent so it can respond or recover.

## Model Rules

The model:

CAN:

- reason
- plan
- select tools
- provide tool parameters
- interpret tool results

The model CANNOT:

- execute arbitrary Android code
- execute arbitrary shell commands
- bypass registered tools
- automatically approve protected actions

## UI

The primary screen defaults to:

- custom SOL identity
- conversation
- microphone, text input, and Send controls
- compact wake status
- confirmation dialogs and progress only when relevant
- a collapsed Setup panel for permissions and Android integration

Avoid unnecessary screens.

One polished agent screen is enough for the MVP.

## Coding Principles

- Kotlin first
- Jetpack Compose UI
- native Android APIs first
- Intents second
- external APIs if necessary
- Accessibility only as fallback
- minimum dependencies
- small modular components
- compile frequently
- do not over-engineer
