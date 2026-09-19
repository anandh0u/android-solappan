# Existing MVP audit — 2026-09-20

Scope: audit and repair already implemented functionality only. No new milestone or stretch feature is authorized.

## Repairs

- Preserve failed Android intent messages instead of replacing them with success descriptions.
- Keep the run status failed when any tool failed, even if the model subsequently answers.
- Validate required fields, supported argument names, strings, integers, ranges and enums before approval or execution.
- Propagate cancellation and check it before tool execution; dismissing the assistant cancels remaining work and denies pending approval. Actions already dispatched to Android cannot be undone.
- Prevent repeated Run taps, refresh speech recognition between attempts, and cancel old auto-dismiss jobs.
- Keep text-only answers and failures visible; allow scrolling within the assistant panel.
- Clear local screen context on hide, destroy and submission; bound assist-text traversal and exclude hidden/password fields. This is not screenshot redaction or a guarantee about provider retention.
- Preserve multiple contact numbers so ambiguous choices fail safely rather than silently choosing one.
- Guard Android-version-specific role APIs and restrict assistant invocation broadcasts; the tile requires the active assistant and unlocking.

## Evidence

| Check | Result |
| --- | --- |
| Debug build | Passed |
| Unit tests | 18 passed |
| Android lint | Passed, warnings remain |
| Install on connected phone | Passed |
| Fresh Open Spotify request | Model/tool round trip completed; actual foreground app not independently confirmed in this audit |
| Fresh nonexistent-app request | UI displayed Failed and the actual app-not-found error |
| Assistant invocation via injected assist key | Inconclusive; observed focus did not establish popup visibility |
| Calls, SMS, alarms, maps, media, screen understanding | Earlier recorded/user-confirmed evidence only; not freshly revalidated end-to-end in this audit |
| Tracked credential check | No detected API key; local.properties remains ignored |

## Remaining limits / manual checks

- Recheck the actual Quick Settings tile or configured system gesture, speech retry, and dismissal during a multi-step workflow on the phone after this update.
- Recheck approved and cancelled call/SMS drafts. Calls still open the dialer; messages still open drafts, not automatic sending.
- Intent acceptance is not proof that navigation started or an alarm was saved. Media-key dispatch is not proof of playback.
- At audit time, accessibility scrolling/tapping and the custom “Hey Sol” wake word were not implemented. The later P1 upgrade added optional restricted accessibility code; see `ISSUES.md`. Wake word remains unimplemented.
- The model may suggest unsupported next steps in prose; only registered tools can execute.
- Screen context is sent to the model for explicit screen questions. Local clearing does not delete remote API records. A build-time API key in an APK is not suitable for public distribution.
- Dependency/string-resource/icon warnings remain; no dependency upgrade or architecture rewrite was attempted.

Stop here pending the user's next instruction. This report does not certify every physical-device workflow as newly tested.
