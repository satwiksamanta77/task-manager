import { Hono } from 'hono';
import type { Env } from '../db';
import { all, first, newId, nowIso, readJson } from '../db';
import { authMiddleware } from '../auth';

type Vars = { Bindings: Env; Variables: { userId: string } };

interface ProjectRow {
  id: string;
  user_id: string;
  name: string;
  color: string | null;
  position: number;
  created_at: string;
}

interface Project {
  id: string;
  name: string;
  color: string | null;
  position: number;
  created_at: string;
}

function toProject(r: ProjectRow): Project {
  return { id: r.id, name: r.name, color: r.color, position: r.position, created_at: r.created_at };
}

const COLOR_RE = /^#[0-9a-fA-F]{6}$/;

export const projectRoutes = new Hono<Vars>();
projectRoutes.use('*', authMiddleware);

/** GET /api/projects — list projects ordered by position. */
projectRoutes.get('/', async (c) => {
  const userId = c.get('userId');
  const rows = await all<ProjectRow>(
    c.env.DB,
    'SELECT * FROM projects WHERE user_id = ? ORDER BY position ASC, created_at ASC',
    userId
  );
  return c.json({ projects: rows.map(toProject) }, 200);
});

/** POST /api/projects — create a project. */
projectRoutes.post('/', async (c) => {
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

  const maxPos = await first<{ maxPos: number | null }>(
    c.env.DB,
    'SELECT MAX(position) AS maxPos FROM projects WHERE user_id = ?',
    userId
  );
  const now = nowIso();
  const id = newId();
  await c.env.DB.prepare('INSERT INTO projects (id, user_id, name, color, position, created_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind(id, userId, name.trim(), typeof color === 'string' ? color : null, (maxPos?.maxPos ?? -1) + 1, now)
    .run();
  const row = await first<ProjectRow>(c.env.DB, 'SELECT * FROM projects WHERE id = ? AND user_id = ?', id, userId);
  if (!row) return c.json({ error: 'Project not found' }, 404);
  return c.json({ project: toProject(row) }, 201);
});

/** PUT /api/projects/:id — partial update. */
projectRoutes.put('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const parsed = await readJson<{ name?: unknown; color?: unknown; position?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { name, color, position } = parsed.body;

  const sets: string[] = [];
  const vals: unknown[] = [];
  if (name !== undefined) {
    if (typeof name !== 'string' || name.trim().length === 0) return c.json({ error: 'Invalid name' }, 400);
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
  if (position !== undefined) {
    if (typeof position !== 'number' || !Number.isInteger(position)) {
      return c.json({ error: 'Invalid position' }, 400);
    }
    sets.push('position = ?');
    vals.push(position);
  }
  if (sets.length === 0) return c.json({ error: 'No fields to update' }, 400);

  vals.push(id, userId);
  const res = await c.env.DB.prepare(`UPDATE projects SET ${sets.join(', ')} WHERE id = ? AND user_id = ?`)
    .bind(...vals)
    .run();
  if ((res.meta.changes ?? 0) === 0) return c.json({ error: 'Project not found' }, 404);

  const row = await first<ProjectRow>(c.env.DB, 'SELECT * FROM projects WHERE id = ? AND user_id = ?', id, userId);
  if (!row) return c.json({ error: 'Project not found' }, 404);
  return c.json({ project: toProject(row) }, 200);
});

/** DELETE /api/projects/:id — delete project; tasks keep existing with project_id nulled. */
projectRoutes.delete('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const existing = await first<{ id: string }>(
    c.env.DB,
    'SELECT id FROM projects WHERE id = ? AND user_id = ?',
    id,
    userId
  );
  if (!existing) return c.json({ error: 'Project not found' }, 404);
  await c.env.DB.batch([
    c.env.DB.prepare('UPDATE tasks SET project_id = NULL WHERE project_id = ? AND user_id = ?').bind(id, userId),
    c.env.DB.prepare('DELETE FROM projects WHERE id = ? AND user_id = ?').bind(id, userId),
  ]);
  return c.json({ ok: true }, 200);
});
