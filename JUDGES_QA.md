# Judge Q&A

## What makes SOL an agent?

The model selects registered tools in a bounded loop, receives structured results, and decides whether more steps are needed. Android owns validation, permissions, approval, and execution.

## Why prefer native tools?

Native APIs and intents offer clearer contracts than screen automation. Optional accessibility fills selected gaps through observation, scrolling, tapping, typing, Back, and Home. It remains app-dependent and requires explicit Android consent.

## Can it run arbitrary generated code?

No. Only registered tools execute. Unknown tools and invalid arguments fail before execution.

## What prevents accidental calls or messages?

Protected actions pause for explicit approval, with a separate review checkbox. The registry independently requires a confirmation grant. Calls use the dialer and messages use a draft; the user retains the final Call or Send action.

## Does the model receive private phone numbers?

The contact lookup tool returns IDs and names, not raw numbers. Numbers are resolved inside Android. Explicit screen context can contain private visible information, so this is not a blanket privacy guarantee.

## Is Hey SOL fully offline?

Wake phrase recognition uses Vosk and a bundled small English model on the device. Android command recognition may use its installed provider, and reasoning uses OpenAI over the network. The foreground wake service consumes microphone/battery resources and needs OEM qualification.

## Does the assistant stay open?

The current implementation keeps the panel available after a command, speaks the answer, and offers a follow-up listening turn. Silence leaves Talk again available. Close SOL during listening, the Close SOL button, or Android dismissal ends the interaction.

## Is this realtime voice?

It is turn-based speech recognition followed by model reasoning and final-answer TTS. Full-duplex streaming, barge-in, and production audio routing are deferred work.

## What does “verified” mean?

An accepted Android intent establishes that the request was dispatched. Screen observation can confirm visible UI state. Neither proves every external outcome. The current [audit](AUDIT.md) separates code/build evidence, historical tests, and pending physical-device checks.

## Is it production ready?

No. It is a technical alpha. The local build contains private API configuration and must not be distributed with a key. Production needs an authenticated gateway, privacy/release work, lifecycle recovery, and broader device testing.

## Why is Supabase not required?

The current runtime does not require cloud persistence. Backend requirements should follow the authentication, credential isolation, and operational needs of a public release rather than the availability of credits.

## What happens offline?

The wake detector can still run locally. Model-driven workflows require connectivity and should show a readable failure. Fresh offline regression remains part of release qualification.

## What comes next?

The [GitHub issues](https://github.com/anandh0u/android-solappan/issues) track production gateway, wake/OEM qualification, accessibility assurance, realtime/lifecycle work, release qualification, and deferred integrations.
