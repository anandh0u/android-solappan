# Instructions for Coding Agents

## Mission

Build a reliable Android AI agent MVP within a 12-hour hackathon.

Reliability and demo quality are more important than architecture perfection or feature count.

## Mandatory Reading

Before modifying code, read:

1. `README.md`
2. `PRD.md`
3. `PROJECT.md`
4. `DECISIONS.md`
5. `ISSUES.md`
6. `BUILD_PLAN.md`

## Session Rule

Only implement the currently requested build session.

Do NOT automatically implement future sessions.

Do NOT work on stretch goals unless explicitly instructed.

## Development Rules

1. Inspect existing code before editing.

2. Do not replace working architecture unnecessarily.

3. Do not perform large refactors unless required to fix a blocker.

4. Keep dependencies minimal.

5. Compile frequently.

6. Fix compilation errors before implementing additional features.

7. Native Android APIs are preferred over UI automation.

8. Android Intents are preferred where appropriate.

9. Accessibility functionality must remain optional for the core MVP.

10. Never hardcode credentials or secrets.

11. Never commit API keys.

12. Every model-triggered action must use a registered tool.

13. Never allow arbitrary model-generated code to execute automatically.

14. Unknown tools must be rejected.

15. Invalid parameters must not crash the application.

16. Consequential actions require user confirmation.

17. Do not start stretch features while P0 issues remain.

18. Update `ISSUES.md` when work is completed or new problems are discovered.

19. Record major architectural changes in `DECISIONS.md`.

20. Preserve working demo paths.

## Implementation Procedure

For each task:

1. Read relevant documentation.
2. Inspect current repository state.
3. Identify the smallest incomplete requirement.
4. Explain the implementation approach.
5. Implement it.
6. Compile.
7. Fix errors.
8. Test the feature.
9. Update issues.
10. Stop before starting unrelated work.

## Do Not Do This

Do not:

- rewrite the project from scratch
- switch frameworks
- add unnecessary design systems
- introduce a complex backend prematurely
- implement dozens of Android tools
- implement future-session functionality early
- modify stable code merely to make it cleaner
- silently ignore errors
- fake completed tool execution

## Priority

Reliability

>

Correct execution

>

Demo experience

>

UX

>

Feature count

>

Code elegance

## Core Agent Principle

The model decides **what should happen**.

The Android runtime decides **what is allowed and how it happens**.
