# Judge Q&A

## Is this just a chatbot opening apps?

No. The model participates in a bounded execution loop: it selects registered tools, receives structured results, and continues planning across multiple actions. Android—not the model—owns validation, permissions, risk, and execution.

## Why not use AccessibilityService for everything?

Native APIs and intents are more deterministic, secure, and demo-reliable. Accessibility remains a future fallback for capabilities without native integration.

## Can the model execute arbitrary code?

No. Only tools in `ToolRegistry` can execute. Unknown names and invalid arguments return structured failures.

## What prevents accidental calls or messages?

Protected tools carry risk metadata. `AgentController` pauses before execution, the user must check a separate review control, and `ToolRegistry` independently rejects execution without a confirmation grant. Calls open `ACTION_DIAL`, and messages open a draft via `ACTION_SENDTO`, so the user retains the final action.

## Does the model see private phone numbers?

No. Contact results expose an internal ID and display name. Number resolution stays inside the Android runtime.

## Why use OpenAI directly from the app?

This is a local hackathon prototype. The key is excluded from Git, but a production release would route model access through an authenticated backend proxy.

## Why is Supabase not used?

The MVP does not require persistent cloud state. Avoiding an unnecessary backend improves reliability and keeps the demonstration focused on agentic mobile automation.

## What happens offline?

The request stops safely, the UI enters Failed state, and the user receives a clear network message. The app does not crash.

## What would you build next?

Outcome observation and verification, a small set of high-value productivity integrations such as calendar and email drafts, and selective accessibility assistance behind the same registry and approval boundary.
