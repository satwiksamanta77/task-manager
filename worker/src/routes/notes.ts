import { Hono } from 'hono';
import type { Env } from '../db';
import { all, first, newId, nowIso, readJson } from '../db';
import { authMiddleware } from '../auth';
import {
  getLabelsForNote,
  parseLabelIds,
  setNoteLabels,
  validateUserLabels,
  type Label,
} from '../labels';

type Vars = { Bindings: Env; Variables: { userId: string } };

interface NoteRow {
  id: string;
  user_id: string;
  title: string;
  content: string;
  color: string;
  pinned: number;
  archived: number;
  deleted_at: string | null;
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
  deleted_at: string | null;
  created_at: string;
  updated_at: string;
  labels: Label[];
}

interface ChecklistItemRow {
  id: string;
  note_id: string;
  text: string;
  checked: number;
  position: number;
}

interface ChecklistItem {
  id: string;
  note_id: string;
  text: string;
  checked: boolean;
  position: number;
}

function toNote(r: NoteRow, labels?: Label[]): Note {
  return {
    id: r.id,
    title: r.title,
    content: r.content,
    color: r.color,
    pinned: r.pinned === 1,
    archived: r.archived === 1,
    deleted_at: r.deleted_at,
    created_at: r.created_at,
    updated_at: r.updated_at,
    labels: labels ?? [],
  };
}

function toChecklistItem(r: ChecklistItemRow): ChecklistItem {
  return { id: r.id, note_id: r.note_id, text: r.text, checked: r.checked === 1, position: r.position };
}

/** Full note: row + labels. */
async function getNoteDetail(db: D1Database, id: string): Promise<Note | null> {
  const row = await first<NoteRow>(db, 'SELECT * FROM notes WHERE id = ?', id);
  if (!row) return null;
  return toNote(row, await getLabelsForNote(db, id));
}

const COLOR_RE = /^#[0-9a-fA-F]{6}$/;

export const notesRoutes = new Hono<Vars>();
notesRoutes.use('*', authMiddleware);

/** GET /api/notes — pinned first, then newest. Deleted notes excluded unless ?trashed=true. */
notesRoutes.get('/', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;
  const archivedParam = c.req.query('archived');
  if (archivedParam !== undefined && archivedParam !== 'true' && archivedParam !== 'false') {
    return c.json({ error: 'Invalid archived filter' }, 400);
  }
  const archived = archivedParam === 'true' ? 1 : 0;
  const trashed = c.req.query('trashed') === 'true';

  const labelParam = c.req.query('label_id');
  if (labelParam !== undefined) {
    const valid = await validateUserLabels(db, userId, [labelParam]);
    if (!valid) return c.json({ error: 'Invalid label_id filter' }, 400);
  }

  let sql = 'SELECT * FROM notes WHERE user_id = ? AND archived = ?';
  const params: unknown[] = [userId, archived];
  if (trashed) {
    sql += ' AND deleted_at IS NOT NULL';
  } else {
    sql += ' AND deleted_at IS NULL';
  }
  if (labelParam !== undefined) {
    sql += ' AND EXISTS (SELECT 1 FROM note_labels nl WHERE nl.note_id = notes.id AND nl.label_id = ?)';
    params.push(labelParam);
  }
  sql += trashed ? ' ORDER BY deleted_at DESC' : ' ORDER BY pinned DESC, created_at DESC';

  const rows = await all<NoteRow>(db, sql, ...params);
  const notes: Note[] = [];
  for (const r of rows) {
    notes.push(toNote(r, await getLabelsForNote(db, r.id)));
  }
  return c.json({ notes }, 200);
});

/** POST /api/notes — create a note. */
notesRoutes.post('/', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;
  const parsed = await readJson<{ title?: unknown; content?: unknown; color?: unknown; label_ids?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { title, content, color, label_ids } = parsed.body;

  if (title !== undefined && typeof title !== 'string') return c.json({ error: 'Invalid title' }, 400);
  if (content !== undefined && typeof content !== 'string') return c.json({ error: 'Invalid content' }, 400);
  if (color !== undefined && (typeof color !== 'string' || !COLOR_RE.test(color))) {
    return c.json({ error: 'Invalid color' }, 400);
  }

  let labelIdVals: string[] = [];
  if (label_ids !== undefined) {
    const parsedIds = parseLabelIds(label_ids);
    if (!parsedIds) return c.json({ error: 'Invalid label_ids' }, 400);
    const valid = await validateUserLabels(db, userId, parsedIds);
    if (!valid) return c.json({ error: 'Unknown label id' }, 400);
    labelIdVals = valid;
  }

  const now = nowIso();
  const id = newId();
  await db
    .prepare(
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
  await setNoteLabels(db, id, labelIdVals);
  const note = await getNoteDetail(db, id);
  if (!note) return c.json({ error: 'Note not found' }, 404);
  return c.json({ note }, 201);
});

/** GET /api/notes/:id */
notesRoutes.get('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const row = await first<NoteRow>(c.env.DB, 'SELECT * FROM notes WHERE id = ? AND user_id = ?', id, userId);
  if (!row) return c.json({ error: 'Note not found' }, 404);
  const note = await getNoteDetail(c.env.DB, id);
  if (!note) return c.json({ error: 'Note not found' }, 404);
  return c.json({ note }, 200);
});

/** PUT /api/notes/:id — partial update. label_ids replaces the label associations. */
notesRoutes.put('/:id', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;
  const id = c.req.param('id');
  const parsed = await readJson<{
    title?: unknown;
    content?: unknown;
    color?: unknown;
    pinned?: unknown;
    archived?: unknown;
    label_ids?: unknown;
  }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { title, content, color, pinned, archived, label_ids } = parsed.body;

  // Validate label ids before touching the database.
  let labelIdVals: string[] | undefined;
  if (label_ids !== undefined) {
    const parsedIds = parseLabelIds(label_ids);
    if (!parsedIds) return c.json({ error: 'Invalid label_ids' }, 400);
    const valid = await validateUserLabels(db, userId, parsedIds);
    if (!valid) return c.json({ error: 'Unknown label id' }, 400);
    labelIdVals = valid;
  }

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
  if (sets.length === 0 && labelIdVals === undefined) return c.json({ error: 'No fields to update' }, 400);

  if (sets.length > 0) {
    sets.push('updated_at = ?');
    vals.push(nowIso(), id, userId);
    const res = await db.prepare(`UPDATE notes SET ${sets.join(', ')} WHERE id = ? AND user_id = ?`)
      .bind(...vals)
      .run();
    if ((res.meta.changes ?? 0) === 0) return c.json({ error: 'Note not found' }, 404);
  } else {
    const exists = await first<{ id: string }>(db, 'SELECT id FROM notes WHERE id = ? AND user_id = ?', id, userId);
    if (!exists) return c.json({ error: 'Note not found' }, 404);
  }
  if (labelIdVals !== undefined) {
    await setNoteLabels(db, id, labelIdVals);
  }

  const note = await getNoteDetail(db, id);
  if (!note) return c.json({ error: 'Note not found' }, 404);
  return c.json({ note }, 200);
});

/** DELETE /api/notes/:id — soft delete unless ?permanent=true. */
notesRoutes.delete('/:id', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;
  const id = c.req.param('id');
  const existing = await first<{ id: string }>(
    db,
    'SELECT id FROM notes WHERE id = ? AND user_id = ?',
    id,
    userId
  );
  if (!existing) return c.json({ error: 'Note not found' }, 404);

  if (c.req.query('permanent') === 'true') {
    await db.batch([
      db.prepare('DELETE FROM note_checklist_items WHERE note_id = ?').bind(id),
      db.prepare('DELETE FROM note_labels WHERE note_id = ?').bind(id),
      db.prepare('DELETE FROM notes WHERE id = ? AND user_id = ?').bind(id, userId),
    ]);
  } else {
    const now = nowIso();
    await db
      .prepare('UPDATE notes SET deleted_at = ?, updated_at = ? WHERE id = ? AND user_id = ?')
      .bind(now, now, id, userId)
      .run();
  }
  return c.json({ ok: true }, 200);
});

/** POST /api/notes/:id/restore — un-delete a soft-deleted note. */
notesRoutes.post('/:id/restore', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;
  const id = c.req.param('id');
  const res = await db
    .prepare('UPDATE notes SET deleted_at = NULL, updated_at = ? WHERE id = ? AND user_id = ? AND deleted_at IS NOT NULL')
    .bind(nowIso(), id, userId)
    .run();
  if ((res.meta.changes ?? 0) === 0) return c.json({ error: 'Note not found' }, 404);
  const note = await getNoteDetail(db, id);
  if (!note) return c.json({ error: 'Note not found' }, 404);
  return c.json({ note }, 200);
});

/** Verify a note belongs to the user; returns its id or null. */
async function ownedNoteId(db: D1Database, userId: string, id: string): Promise<string | null> {
  const row = await first<{ id: string }>(db, 'SELECT id FROM notes WHERE id = ? AND user_id = ?', id, userId);
  return row ? row.id : null;
}

/** GET /api/notes/:id/items — checklist items ordered by position. */
notesRoutes.get('/:id/items', async (c) => {
  const userId = c.get('userId');
  const noteId = c.req.param('id');
  if (!(await ownedNoteId(c.env.DB, userId, noteId))) return c.json({ error: 'Note not found' }, 404);
  const rows = await all<ChecklistItemRow>(
    c.env.DB,
    'SELECT id, note_id, text, checked, position FROM note_checklist_items WHERE note_id = ? ORDER BY position ASC',
    noteId
  );
  return c.json({ items: rows.map(toChecklistItem) }, 200);
});

/** POST /api/notes/:id/items — add a checklist item. */
notesRoutes.post('/:id/items', async (c) => {
  const userId = c.get('userId');
  const noteId = c.req.param('id');
  if (!(await ownedNoteId(c.env.DB, userId, noteId))) return c.json({ error: 'Note not found' }, 404);

  const parsed = await readJson<{ text?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { text } = parsed.body;
  if (typeof text !== 'string' || text.trim().length === 0) {
    return c.json({ error: 'Text is required' }, 400);
  }

  const maxPos = await first<{ maxPos: number | null }>(
    c.env.DB,
    'SELECT MAX(position) AS maxPos FROM note_checklist_items WHERE note_id = ?',
    noteId
  );
  const id = newId();
  await c.env.DB.prepare('INSERT INTO note_checklist_items (id, note_id, text, checked, position) VALUES (?, ?, ?, 0, ?)')
    .bind(id, noteId, text.trim(), (maxPos?.maxPos ?? -1) + 1)
    .run();
  const row = await first<ChecklistItemRow>(
    c.env.DB,
    'SELECT id, note_id, text, checked, position FROM note_checklist_items WHERE id = ?',
    id
  );
  if (!row) return c.json({ error: 'Checklist item not found' }, 404);
  return c.json({ item: toChecklistItem(row) }, 201);
});

async function findOwnedItem(
  db: D1Database,
  userId: string,
  noteId: string,
  itemId: string
): Promise<ChecklistItemRow | null> {
  return first<ChecklistItemRow>(
    db,
    `SELECT i.id, i.note_id, i.text, i.checked, i.position FROM note_checklist_items i
     JOIN notes n ON n.id = i.note_id
     WHERE i.id = ? AND i.note_id = ? AND n.user_id = ?`,
    itemId,
    noteId,
    userId
  );
}

/** PUT /api/notes/:id/items/:itemId — update text, checked state, and/or position. */
notesRoutes.put('/:id/items/:itemId', async (c) => {
  const userId = c.get('userId');
  const noteId = c.req.param('id');
  const itemId = c.req.param('itemId');
  const existing = await findOwnedItem(c.env.DB, userId, noteId, itemId);
  if (!existing) return c.json({ error: 'Checklist item not found' }, 404);

  const parsed = await readJson<{ text?: unknown; checked?: unknown; position?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { text, checked, position } = parsed.body;

  const sets: string[] = [];
  const vals: unknown[] = [];
  if (text !== undefined) {
    if (typeof text !== 'string' || text.trim().length === 0) return c.json({ error: 'Invalid text' }, 400);
    sets.push('text = ?');
    vals.push(text.trim());
  }
  if (checked !== undefined) {
    if (typeof checked !== 'boolean') return c.json({ error: 'Invalid checked value' }, 400);
    sets.push('checked = ?');
    vals.push(checked ? 1 : 0);
  }
  if (position !== undefined) {
    if (typeof position !== 'number' || !Number.isInteger(position)) {
      return c.json({ error: 'Invalid position' }, 400);
    }
    sets.push('position = ?');
    vals.push(position);
  }
  if (sets.length > 0) {
    vals.push(itemId);
    await c.env.DB.prepare(`UPDATE note_checklist_items SET ${sets.join(', ')} WHERE id = ?`)
      .bind(...vals)
      .run();
  }
  const row = await first<ChecklistItemRow>(
    c.env.DB,
    'SELECT id, note_id, text, checked, position FROM note_checklist_items WHERE id = ?',
    itemId
  );
  if (!row) return c.json({ error: 'Checklist item not found' }, 404);
  return c.json({ item: toChecklistItem(row) }, 200);
});

/** DELETE /api/notes/:id/items/:itemId */
notesRoutes.delete('/:id/items/:itemId', async (c) => {
  const userId = c.get('userId');
  const noteId = c.req.param('id');
  const itemId = c.req.param('itemId');
  const existing = await findOwnedItem(c.env.DB, userId, noteId, itemId);
  if (!existing) return c.json({ error: 'Checklist item not found' }, 404);
  await c.env.DB.prepare('DELETE FROM note_checklist_items WHERE id = ?').bind(itemId).run();
  return c.json({ ok: true }, 200);
});
