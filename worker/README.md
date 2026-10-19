# TaskManager API (Cloudflare Worker)

Hono + Cloudflare Workers + D1 backend for the TaskManager app. Implements `../api-contract.md` exactly.

## Prerequisites

- Node.js 18+
- A Cloudflare account with the `taskmanager-db` D1 database (see `wrangler.toml`)

## Local development

```bash
npm install

# Apply migrations to the local D1 database
npx wrangler d1 migrations apply taskmanager-db --local

# Start the dev server (http://localhost:8787)
npm run dev
```

## Deploy

```bash
# Apply migrations to the remote D1 database
npx wrangler d1 migrations apply taskmanager-db --remote

# Deploy the worker
npm run deploy
```

## Typecheck

```bash
npm run typecheck
```

## API

Base URL: `https://taskmanager-api.<account-subdomain>.workers.dev/api`

All endpoints except `POST /api/auth/register` and `POST /api/auth/login` require
`Authorization: Bearer <token>`. Errors are `{ "error": "message" }`.
See `../api-contract.md` for the full contract.
