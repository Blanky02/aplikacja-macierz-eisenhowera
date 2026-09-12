# Fokus Bank API

Cloudflare Worker responsible for the optional bank connection. The Android app must never receive Salt Edge secrets or bank credentials.

## What is included

- Salt Edge Partners API adapter
- Google ID token login and e-mail magic-link login
- HS256 access tokens issued by the Worker
- authenticated API contract and development-only `X-Fokus-Dev-User` header
- D1 schema for users, consents, connections, accounts, transactions and sync runs
- normalized transaction model with transparent Eisenhower quadrant suggestions
- manual sync endpoint and daily scheduled sync
- provider list endpoint for country-specific bank selection
- R2 binding reserved for user-requested exports/backups

## Before using a real bank

Salt Edge requires a Partner account, Service API key and production onboarding. New accounts start with fake/sandbox providers; production requires provider review and callback/signature configuration. Keep the following values in Cloudflare secrets, never in Git:

```bash
wrangler secret put SALT_EDGE_APP_ID
wrangler secret put SALT_EDGE_SECRET
wrangler secret put AUTH_JWT_SECRET
wrangler secret put SALTEDGE_CALLBACK_PUBLIC_KEY
```

Create infrastructure:

```bash
wrangler d1 create fokus-bank-db
wrangler r2 bucket create fokus-backups
```

Put the returned D1 database id into `wrangler.toml`, then apply the migration:

```bash
wrangler d1 migrations apply fokus-bank-db --local
wrangler dev
```

For a remote environment use `--remote` after creating the production D1 database. Do not use the development header in production.

## API contract

All `/api/bank/*` endpoints require a bearer JWT with `sub`, `email` and optional `exp` claims. During local development only, the following headers are accepted:

```text
X-Fokus-Dev-User: demo-user
X-Fokus-Dev-Email: demo@example.test
```

Endpoints:

- `GET /api/health`
- `GET /api/bank/providers?country=PL`
- `POST /api/bank/connect` with optional `{ "historyDays": 3650 }` — requests the largest configured history window and returns a Salt Edge authorization URL
- `GET /api/bank/connections`
- `POST /api/bank/sync`
- `GET /api/bank/transactions?limit=250`
- `PATCH /api/bank/transactions/:id/quadrant` with `{ "quadrantIndex": 0..3 }` — records the user's manual confirmation
- `DELETE /api/bank/connections/:id` — revokes the Salt Edge partner consent before marking the connection disconnected
- `POST /api/bank/callback` — verifies the Salt Edge RSA callback signature and schedules a sync

Example local request:

```bash
curl http://localhost:8787/api/bank/providers?country=PL \
  -H 'X-Fokus-Dev-User: demo-user' \
  -H 'X-Fokus-Dev-Email: demo@example.test'
```

## Security boundaries

- The mobile app talks only to this Worker.
- Salt Edge credentials are Worker secrets.
- Raw provider payloads are not stored in R2 or returned to the mobile app.
- A transaction's suggested quadrant is not treated as a user decision; confirmation is stored separately in D1 in the next mobile-sync step.
- Callback signature verification is implemented for the RSA public key from Salt Edge onboarding. Outgoing request signing remains a production-onboarding task because Salt Edge enables it for live clients.
- Configure the Google OAuth client id, a verified e-mail sender such as Resend, the Android deep-link allowlist and the production callback URL before real users are enabled.
