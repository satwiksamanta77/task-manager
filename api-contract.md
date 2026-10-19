# TaskManager API Contract v2

Source of truth for the Cloudflare Worker backend (`worker/`), the Android app (`android/`), and the web app (`web/`).
All sides must implement this exactly.

Base URL: `https://<worker-name>.<account-subdomain>.workers.dev/api`

v2 adds: universal search, projects, labels, recurring tasks, soft-delete trash, note checklists.

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
"project_id": string|null, "recurrence": "none"|"daily"|"weekly"|"monthly",
"deleted_at": string|null, "created_at", "updated_at",
"labels": [{ "id", "name", "color": string|null, "created_at" }],
"subtasks": [Subtask] (only on detail/create/update/search responses) }`

### GET /api/tasks (auth)
Query: `?status=todo|done` (optional), `?project_id=` (optional), `?label_id=` (optional),
`?trashed=true` (optional — when set, returns only soft-deleted tasks ordered by deleted_at DESC;
default returns only non-deleted, newest first)
- 200: `{ "tasks": [Task] }` — each task includes its labels, without subtasks array.
- 400: invalid status/project_id/label_id filter.

### POST /api/tasks (auth)
Body: `{ "title": string (required), "description"?, "priority"?, "due_at"?, "reminder_at"?,
"project_id"?: string|null, "recurrence"?: "none"|"daily"|"weekly"|"monthly" (default "none"),
"label_ids"?: [string] }`
- `project_id` must belong to the user (400 otherwise); `label_ids` must all belong to the user (400 otherwise).
- 201: `{ "task": Task }` (with empty subtasks array and labels)

### GET /api/tasks/:id (auth)
- 200: `{ "task": Task }` (with subtasks array and labels)
- 404: not found or not owned by user

### PUT /api/tasks/:id (auth)
Body: any of `{ "title", "description", "status", "priority", "due_at", "reminder_at",
"project_id", "recurrence", "label_ids" }` — `label_ids` replaces all label associations.
- When `status` is set to `"done"` on a task with `recurrence != "none"` that was not already done,
  the next occurrence is spawned (new id, status `todo`, due date shifted +1d/+7d/+1mo —
  from now when no due date — reminder shifted likewise, labels and subtasks copied unchecked)
  and the response is `{ "task": Task (completed), "next_task": Task }`.
- 200: `{ "task": Task }` (with subtasks array and labels)
- 400: invalid field / unknown label id; 404: not found or not owned

### DELETE /api/tasks/:id (auth)
- Soft delete (sets `deleted_at`) → 200 `{ "ok": true }`.
- `?permanent=true`: hard delete incl. subtasks and label associations.

### POST /api/tasks/:id/restore (auth)
- Clears `deleted_at` on a soft-deleted task. 200: `{ "task": Task }`; 404 if not deleted/not owned.

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
"pinned": bool, "archived": bool, "deleted_at": string|null, "created_at", "updated_at",
"labels": [{ "id", "name", "color": string|null, "created_at" }] }`

### GET /api/notes (auth)
Query: `?archived=true|false` (default false), `?label_id=` (optional),
`?trashed=true` (optional — only soft-deleted notes, ordered by deleted_at DESC; default excludes deleted)
- 200: `{ "notes": [Note] }` — pinned first, then newest; each note includes its labels.

### POST /api/notes (auth)
Body: `{ "title"?, "content"?, "color"?, "label_ids"?: [string] }`
- 201: `{ "note": Note }` (with labels)

### GET /api/notes/:id (auth)
- 200: `{ "note": Note }` (with labels), 404 if not found/owned.

### PUT /api/notes/:id (auth)
Body: `{ "title"?, "content"?, "color"?, "pinned"?, "archived"?, "label_ids"? }` — `label_ids` replaces associations.
- 200: `{ "note": Note }` (with labels); 400 on unknown label id.

### DELETE /api/notes/:id (auth)
- Soft delete (sets `deleted_at`) → 200 `{ "ok": true }`.
- `?permanent=true`: hard delete incl. checklist items and label associations.

### POST /api/notes/:id/restore (auth)
- Clears `deleted_at`. 200: `{ "note": Note }`; 404 if not deleted/not owned.

## Checklist items (per note)

ChecklistItem: `{ "id", "note_id", "text": string, "checked": bool, "position": int }`

### GET /api/notes/:id/items (auth)
- 200: `{ "items": [ChecklistItem] }` ordered by position; 404 if note not found/owned.

### POST /api/notes/:id/items (auth)
Body: `{ "text": string (required) }`
- 201: `{ "item": ChecklistItem }`

### PUT /api/notes/:id/items/:itemId (auth)
Body: `{ "text"?, "checked"?, "position"? }`
- 200: `{ "item": ChecklistItem }`; 404 if not found/not owned (via note).

### DELETE /api/notes/:id/items/:itemId (auth)
- 200: `{ "ok": true }`

## Projects

Project: `{ "id", "name": string, "color": string|null, "position": int, "created_at" }`

### GET /api/projects (auth)
- 200: `{ "projects": [Project] }` ordered by position.

### POST /api/projects (auth)
Body: `{ "name": string (required), "color"?: hex|null }`
- 201: `{ "project": Project }`

### PUT /api/projects/:id (auth)
Body: `{ "name"?, "color"?, "position"? }`
- 200: `{ "project": Project }`; 404 if not found/owned.

### DELETE /api/projects/:id (auth)
- 200: `{ "ok": true }` — tasks in the project keep existing with `project_id` nulled.

## Labels

Label: `{ "id", "name": string, "color": string|null, "created_at" }`

### GET /api/labels (auth)
- 200: `{ "labels": [Label] }` ordered by name.

### POST /api/labels (auth)
Body: `{ "name": string (required), "color"?: hex|null }`
- 201: `{ "label": Label }`; 409 if a label with that name already exists for the user.

### PUT /api/labels/:id (auth)
Body: `{ "name"?, "color"? }`
- 200: `{ "label": Label }`; 409 on name conflict; 404 if not found/owned.

### DELETE /api/labels/:id (auth)
- 200: `{ "ok": true }` — also removes its task/note associations.

## Universal search

### GET /api/search (auth)
Query: `?q=` (required, min 2 chars)
- 200: `{ "tasks": [Task], "notes": [Note] }` — max 20 each. Tasks match title/description;
  a subtask title match includes its parent task (with subtasks embedded). Notes match title/content.
  Deleted items are excluded. Tasks carry subtasks + labels; notes carry labels.
- 400: query shorter than 2 characters.

## Schedule

### GET /api/schedule/upcoming (auth)
Query: `?days=7` (default 7, max 30)
- 200: `{ "items": [ { "kind": "task", "id", "title", "due_at", "reminder_at", "priority" } ] }`
- Only non-deleted tasks with status `todo` and `due_at` (or `reminder_at`) within the window, sorted by date.

## Errors

All errors: `{ "error": "human readable message" }` with the HTTP status above.
