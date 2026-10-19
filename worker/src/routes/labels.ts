import { Hono } from 'hono';
import type { Env } from '../db';
import { all, first, newId, nowIso, readJson } from '../db';
import { authMiddleware } from '../auth';
import { toLabel, type LabelRow } from '../labels';

type Vars = { Bindings: Env; Variables: { userId: string } };

const COLOR_RE = /^#[0-9a-fA-F]{6}$/;

export const labelRoutes = new Hono<Vars>();
labelRoutes.use('*', authMiddleware);

/** GET /api/labels — list labels ordered by name. */
labelRoutes.get('/', async (c) => {
  const userId = c.get('userId');
  const rows = await all<LabelRow>(
    c.env.DB,
    'SELECT id, user_id, name, color, created_at FROM labels WHERE user_id = ? ORDER BY name ASC',
    userId
  );
  return c.json({ labels: rows.map(toLabel) }, 200);
});

/** POST /api/labels — create a label. Name must be unique per user. */
labelRoutes.post('/', async (c) => {
  const userId = c.get('userId');
  const parsed = await readJson<{ name?: unknown; color?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { name, color } = parsed.body;

  if (typeof name !== 'string' || name.trim().length === 0) {
    return c.json({ error: 'Name is required' }, 400);
  }
  if (color !== undefined && color !== null && (typeof color !== 'string' || !COLOR_RE.test(color))) {
    return c.json({ error: 'Invalid color' }, 400);
  }

  const dup = await first<{ id: string }>(
    c.env.DB,
    'SELECT id FROM labels WHERE user_id = ? AND name = ?',
    userId,
    name.trim()
  );
  if (dup) return c.json({ error: 'Label already exists' }, 409);

  const now = nowIso();
  const id = newId();
  await c.env.DB.prepare('INSERT INTO labels (id, user_id, name, color, created_at) VALUES (?, ?, ?, ?, ?)')
    .bind(id, userId, name.trim(), typeof color === 'string' ? color : null, now)
    .run();
  const row = await first<LabelRow>(
    c.env.DB,
    'SELECT id, user_id, name, color, created_at FROM labels WHERE id = ? AND user_id = ?',
    id,
    userId
  );
  if (!row) return c.json({ error: 'Label not found' }, 404);
  return c.json({ label: toLabel(row) }, 201);
});

/** PUT /api/labels/:id — partial update. */
labelRoutes.put('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const parsed = await readJson<{ name?: unknown; color?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { name, color } = parsed.body;

  const sets: string[] = [];
  const vals: unknown[] = [];
  if (name !== undefined) {
    if (typeof name !== 'string' || name.trim().length === 0) return c.json({ error: 'Invalid name' }, 400);
    const dup = await first<{ id: string }>(
      c.env.DB,
      'SELECT id FROM labels WHERE user_id = ? AND name = ? AND id != ?',
      userId,
      name.trim(),
      id
    );
    if (dup) return c.json({ error: 'Label already exists' }, 409);
    sets.push('name = ?');
    vals.push(name.trim());
  }
  if (color !== undefined) {
    if (color !== null && (typeof color !== 'string' || !COLOR_RE.test(color))) {
      return c.json({ error: 'Invalid color' }, 400);
    }
    sets.push('color = ?');
    vals.push(color);
  }
  if (sets.length === 0) return c.json({ error: 'No fields to update' }, 400);

  vals.push(id, userId);
  const res = await c.env.DB.prepare(`UPDATE labels SET ${sets.join(', ')} WHERE id = ? AND user_id = ?`)
    .bind(...vals)
    .run();
  if ((res.meta.changes ?? 0) === 0) return c.json({ error: 'Label not found' }, 404);

  const row = await first<LabelRow>(
    c.env.DB,
    'SELECT id, user_id, name, color, created_at FROM labels WHERE id = ? AND user_id = ?',
    id,
    userId
  );
  if (!row) return c.json({ error: 'Label not found' }, 404);
  return c.json({ label: toLabel(row) }, 200);
});

/** DELETE /api/labels/:id — delete label and its task/note associations. */
labelRoutes.delete('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const existing = await first<{ id: string }>(
    c.env.DB,
    'SELECT id FROM labels WHERE id = ? AND user_id = ?',
    id,
    userId
  );
  if (!existing) return c.json({ error: 'Label not found' }, 404);
  await c.env.DB.batch([
    c.env.DB.prepare('DELETE FROM task_labels WHERE label_id = ?').bind(id),
    c.env.DB.prepare('DELETE FROM note_labels WHERE label_id = ?').bind(id),
    c.env.DB.prepare('DELETE FROM labels WHERE id = ? AND user_id = ?').bind(id, userId),
  ]);
  return c.json({ ok: true }, 200);
});
