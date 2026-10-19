# TaskManager API Contract v1

Source of truth for the Cloudflare Worker backend (`worker/`) and the Android app (`android/`).
Both sides must implement this exactly.

Base URL: `https://<worker-name>.<account-subdomain>.workers.dev/api`

## Auth

All endpoints except `POST /api/auth/register` and `POST /api/auth/login` require:

```
Authorization: Bearer <token>
```

Token: 64-char hex session token, 30-day expiry, stored in D1 `sessions`.
IDs: `crypto.randomUUID()`. Timestamps: ISO-8601 UTC strings.

### POST /api/auth/register
Body: `{ "email": string, "password": string, "name": string }`
- 201: `{ "token": string, "user": User }`
- 400: invalid email / password shorter than 6 chars / missing name
- 409: email already registered

### POST /api/auth/login
Body: `{ "email": string, "password": string }`
- 200: `{ "token": string, "user": User }`
- 401: invalid credentials

### POST /api/auth/logout (auth)
- 200: `{ "ok": true }` — deletes the session token.

### GET /api/auth/me (auth)
- 200: `{ "user": User }`

## Tasks

Task: `{ "id", "title", "description": string|null, "status": "todo"|"done",
"priority": "low"|"medium"|"high", "due_at": string|null, "reminder_at": string|null,
"created_at", "updated_at", "subtasks": [Subtask] (only on detail/create/update responses) }`

### GET /api/tasks (auth)
Query: `?status=todo|done` (optional filter)
- 200: `{ "tasks": [Task] }` — newest first, without subtasks array.

### POST /api/tasks (auth)
Body: `{ "title": string (required), "description"?, "priority"?, "due_at"?, "reminder_at"? }`
- 201: `{ "task": Task }` (with empty subtasks array)

### GET /api/tasks/:id (auth)
- 200: `{ "task": Task }` (with subtasks array)
- 404: not found or not owned by user

### PUT /api/tasks/:id (auth)
Body: any of `{ "title", "description", "status", "priority", "due_at", "reminder_at" }`
- 200: `{ "task": Task }` (with subtasks array)
- 404: not found or not owned

### DELETE /api/tasks/:id (auth)
- 200: `{ "ok": true }` — deletes task and its subtasks.

## Subtasks

Subtask: `{ "id", "task_id", "title", "done": bool, "position": int }`

### POST /api/tasks/:id/subtasks (auth)
Body: `{ "title": string (required) }`
- 201: `{ "subtask": Subtask }`
- 404: task not found or not owned

### PUT /api/subtasks/:id (auth)
Body: `{ "title"?, "done"? }`
- 200: `{ "subtask": Subtask }`
- 404: not found or not owned (via parent task)

### DELETE /api/subtasks/:id (auth)
- 200: `{ "ok": true }`

## Notes (Google Keep style)

Note: `{ "id", "title": string, "content": string, "color": hex string like "#FFF7AE",
"pinned": bool, "archived": bool, "created_at", "updated_at" }`

### GET /api/notes (auth)
Query: `?archived=true|false` (default false)
- 200: `{ "notes": [Note] }` — pinned first, then newest.

### POST /api/notes (auth)
Body: `{ "title"?, "content"?, "color"? }`
- 201: `{ "note": Note }`

### GET /api/notes/:id (auth)
- 200: `{ "note": Note }`, 404 if not found/owned.

### PUT /api/notes/:id (auth)
Body: `{ "title"?, "content"?, "color"?, "pinned"?, "archived"? }`
- 200: `{ "note": Note }`

### DELETE /api/notes/:id (auth)
- 200: `{ "ok": true }`

## Schedule

### GET /api/schedule/upcoming (auth)
Query: `?days=7` (default 7, max 30)
- 200: `{ "items": [ { "kind": "task", "id", "title", "due_at", "reminder_at", "priority" } ] }`
- Only tasks with status `todo` and `due_at` (or `reminder_at`) within the window, sorted by date.

## Errors

All errors: `{ "error": "human readable message" }` with the HTTP status above.
