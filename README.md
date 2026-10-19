# TaskManager

A task manager + Google Keep-style notes app.

- **Android app** (`android/`) — Kotlin + Jetpack Compose, Material 3, dark-first premium theme.
- **Backend** (`worker/`) — Cloudflare Worker (Hono) + D1 database.
- **Releases** — pushing a `v*` tag builds the APK with GitHub Actions and publishes it to GitHub Releases.

## Features

- Email/password **register & login** (token sessions, PBKDF2-hashed passwords)
- **Tasks** with priority, due dates, reminders, and done/todo states
- **Subtasks** — checklists inside every task
- **Schedule** — upcoming due dates & reminders in one view
- **Keep-style notes** — colored cards, pinning, archive, search, staggered grid
- **Profile** — view account, log out

## Project layout

```
android/   # Kotlin/Compose Android app (package com.taskmanager.app)
worker/    # Cloudflare Worker API (Hono + D1)
  src/         # index, auth, routes (auth/tasks/notes/schedule), db helpers
  migrations/  # D1 schema (0001_init.sql)
.github/   # release workflow: builds APK on v* tags
```

The API both sides speak is documented in [`api-contract.md`](api-contract.md).

## Backend setup

Prerequisites: a Cloudflare account and [wrangler](https://developers.cloudflare.com/workers/wrangler/).

```bash
cd worker
npm install
# create the D1 database (once)
npx wrangler d1 create taskmanager-db
# put the returned database_id into wrangler.toml, then:
npx wrangler d1 migrations apply taskmanager-db --remote
npx wrangler deploy
```

Your API will live at `https://<worker-name>.<your-subdomain>.workers.dev`.
Point the app at it in `android/.../data/api/ApiClient.kt` (`BASE_URL`).

## Android build

Prerequisites: JDK 17, Gradle 8.7+, Android SDK (platform 34, build-tools 34.0.0).

```bash
cd android
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or let CI do it: push a tag like `v1.0.0` and the APK appears under
**Releases** on GitHub a few minutes later.

## API

Base URL: `https://<worker>.workers.dev/api` — see [`api-contract.md`](api-contract.md)
for every endpoint, model, and error shape.
