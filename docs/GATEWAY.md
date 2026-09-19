# Private-beta gateway

Implemented locally, **not deployed or qualified for public release**. The debug demo still uses the developer key. Never distribute that APK.

## Architecture

Android → Supabase email/password Auth → authenticated Edge Function → OpenAI Responses → registered Android tools and local approval checks.

Android encrypts access/refresh tokens with Android Keystore AES-GCM in no-backup storage. It never saves the password. A cross-process file lock serializes token refresh between chat and the assistant. Release builds force gateway mode and an empty OpenAI key; debug mode is explicitly selectable.

The gateway validates the bearer token against Supabase Auth, requires verified non-anonymous users, and checks a server-managed beta allowlist. RLS and grants prevent clients accessing quota and conversation-owner tables. The service role alone can reserve quota. Limits are 12 model requests/minute/user, 100/day/user and 1000/day across the beta. Every tool-loop model round trip counts, including failed provider requests. These are request caps, **not dollar spending guarantees**; also set provider project budgets/alerts and monitor usage.

The server fixes the model to GPT-6 Astra, low reasoning, sequential registered tools and 4096 output tokens. Request bodies are limited to 2 MB. Continuation IDs must belong to the authenticated user and expire after 24 hours. No hosted tools or arbitrary code execution are enabled. Client-supplied function schemas do not authorize device actions: the Android registry remains authoritative.

## Deployment (owner action)

1. Authenticate locally with `npx supabase login`. A database password or publishable key is not a deployment token.
2. Link the intended project: `npx supabase link --project-ref YOUR_PROJECT_REF`. Supply database credentials only at the CLI prompt.
3. Review the migration, take any required database backup, then run `npx supabase db push`.
4. In the project's Edge Function secrets dashboard, set `OPENAI_API_KEY`. Never put it in Android release configuration, source control or issue comments. Supabase supplies the URL and service-role environment values to hosted functions.
5. Run `npx supabase functions deploy agent-gateway`. JWT gateway verification is disabled in config because the handler independently validates every bearer token through Auth; do not remove that check.
6. Create/invite an email-verified test user in Supabase Auth. In the SQL editor, enable only that actual user's UUID:

```sql
insert into public.sol_beta_users(user_id, enabled)
values ('REPLACE_WITH_VERIFIED_AUTH_USER_UUID', true)
on conflict(user_id) do update set enabled = true;
```

7. Add the following to ignored `local.properties`, rebuild, open Setup and sign in:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLIC_PUBLISHABLE_KEY
USE_GATEWAY=true
```

8. Verify: invalid tokens fail; unapproved users cannot spend credits; the approved user completes text and multi-step tool workflows; another account cannot reuse a response ID; quota exhaustion fails safely; expired sessions refresh across both Android processes; sign-out clears local credentials. Test offline/reconnect and reinstall behavior. Inspect the release APK for secrets before distribution.

## Privacy and remaining qualification

Prompts, tool results and explicitly requested screen context transit Supabase and OpenAI. The function does not log payloads or save them in SQL. SQL retains quota counters and response IDs; expired rows are opportunistically removed on allowed requests. OpenAI Responses storage is enabled for continuation support: local deletion/sign-out is not provider-side deletion. Provider retention, deletion/export procedures and a user-facing privacy policy still require release review.

Local tests cover policy and mocked authentication/ownership failures, not hosted Auth, database concurrency, RLS deployment or a real signed-in phone workflow. Track those release gates in GitHub issues #1 and #4. Voice/OEM behavior, screen-action assurance and full-duplex audio remain separate gates (#2, #3, #6).
