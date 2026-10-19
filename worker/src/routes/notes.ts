import { Hono } from 'hono';
import type { Env } from '../db';
import { all, first, newId, nowIso, readJson } from '../db';
import { authMiddleware } from '../auth';

type Vars = { Bindings: Env; Variables: { userId: string } };

interface NoteRow {
  id: string;
  user_id: string;
  title: string;
  content: string;
  color: string;
  pinned: number;
  archived: number;
  created_at: string;
  updated_at: string;
}

interface Note {
  id: string;
  title: string;
  content: string;
  color: string;
  pinned: boolean;
  archived: boolean;
  created_at: string;
  updated_at: string;
}

function toNote(r: NoteRow): Note {
  return {
    id: r.id,
    title: r.title,
    content: r.content,
    color: r.color,
    pinned: r.pinned === 1,
    archived: r.archived === 1,
    created_at: r.created_at,
    updated_at: r.updated_at,
  };
}

const COLOR_RE = /^#[0-9a-fA-F]{6}$/;

export const notesRoutes = new Hono<Vars>();
notesRoutes.use('*', authMiddleware);

/** GET /api/notes — pinned first, then newest. */
notesRoutes.get('/', async (c) => {
  const userId = c.get('userId');
  const archivedParam = c.req.query('archived');
  if (archivedParam !== undefined && archivedParam !== 'true' && archivedParam !== 'false') {
    return c.json({ error: 'Invalid archived filter' }, 400);
  }
  const archived = archivedParam === 'true' ? 1 : 0;
  const rows = await all<NoteRow>(
    c.env.DB,
    'SELECT * FROM notes WHERE user_id = ? AND archived = ? ORDER BY pinned DESC, created_at DESC',
    userId,
    archived
  );
  return c.json({ notes: rows.map(toNote) }, 200);
});

/** POST /api/notes — create a note. */
notesRoutes.post('/', async (c) => {
  const userId = c.get('userId');
  const parsed = await readJson<{ title?: unknown; content?: unknown; color?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { title, content, color } = parsed.body;

  if (title !== undefined && typeof title !== 'string') return c.json({ error: 'Invalid title' }, 400);
  if (content !== undefined && typeof content !== 'string') return c.json({ error: 'Invalid content' }, 400);
  if (color !== undefined && (typeof color !== 'string' || !COLOR_RE.test(color))) {
    return c.json({ error: 'Invalid color' }, 400);
  }

  const now = nowIso();
  const id = newId();
  await c.env.DB.prepare(
    "INSERT INTO notes (id, user_id, title, content, color, pinned, archived, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 0, 0, ?, ?)"
  )
    .bind(
      id,
      userId,
      typeof title === 'string' ? title : '',
      typeof content === 'string' ? content : '',
      typeof color === 'string' ? color : '#FFF7AE',
      now,
      now
    )
    .run();
  const row = await first<NoteRow>(c.env.DB, 'SELECT * FROM notes WHERE id = ? AND user_id = ?', id, userId);
  if (!row) return c.json({ error: 'Note not found' }, 404);
  return c.json({ note: toNote(row) }, 201);
});

/** GET /api/notes/:id */
notesRoutes.get('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const row = await first<NoteRow>(c.env.DB, 'SELECT * FROM notes WHERE id = ? AND user_id = ?', id, userId);
  if (!row) return c.json({ error: 'Note not found' }, 404);
  return c.json({ note: toNote(row) }, 200);
});

/** PUT /api/notes/:id — partial update. */
notesRoutes.put('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const parsed = await readJson<{
    title?: unknown;
    content?: unknown;
    color?: unknown;
    pinned?: unknown;
    archived?: unknown;
  }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { title, content, color, pinned, archived } = parsed.body;

  const sets: string[] = [];
  const vals: unknown[] = [];
  if (title !== undefined) {
    if (typeof title !== 'string') return c.json({ error: 'Invalid title' }, 400);
    sets.push('title = ?');
    vals.push(title);
  }
  if (content !== undefined) {
    if (typeof content !== 'string') return c.json({ error: 'Invalid content' }, 400);
    sets.push('content = ?');
    vals.push(content);
  }
  if (color !== undefined) {
    if (typeof color !== 'string' || !COLOR_RE.test(color)) return c.json({ error: 'Invalid color' }, 400);
    sets.push('color = ?');
    vals.push(color);
  }
  if (pinned !== undefined) {
    if (typeof pinned !== 'boolean') return c.json({ error: 'Invalid pinned value' }, 400);
    sets.push('pinned = ?');
    vals.push(pinned ? 1 : 0);
  }
  if (archived !== undefined) {
    if (typeof archived !== 'boolean') return c.json({ error: 'Invalid archived value' }, 400);
    sets.push('archived = ?');
    vals.push(archived ? 1 : 0);
  }
  if (sets.length === 0) return c.json({ error: 'No fields to update' }, 400);

  sets.push('updated_at = ?');
  vals.push(nowIso(), id, userId);
  const res = await c.env.DB.prepare(`UPDATE notes SET ${sets.join(', ')} WHERE id = ? AND user_id = ?`)
    .bind(...vals)
    .run();
  if ((res.meta.changes ?? 0) === 0) return c.json({ error: 'Note not found' }, 404);

  const row = await first<NoteRow>(c.env.DB, 'SELECT * FROM notes WHERE id = ? AND user_id = ?', id, userId);
  if (!row) return c.json({ error: 'Note not found' }, 404);
  return c.json({ note: toNote(row) }, 200);
});

/** DELETE /api/notes/:id */
notesRoutes.delete('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const res = await c.env.DB.prepare('DELETE FROM notes WHERE id = ? AND user_id = ?').bind(id, userId).run();
  if ((res.meta.changes ?? 0) === 0) return c.json({ error: 'Note not found' }, 404);
  return c.json({ ok: true }, 200);
});
