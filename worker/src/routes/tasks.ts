import { Hono } from 'hono';
import type { Env } from '../db';
import { all, first, newId, nowIso, readJson } from '../db';
import { authMiddleware } from '../auth';

type Vars = { Bindings: Env; Variables: { userId: string } };

type TaskStatus = 'todo' | 'done';
type TaskPriority = 'low' | 'medium' | 'high';

interface TaskRow {
  id: string;
  user_id: string;
  title: string;
  description: string | null;
  status: string;
  priority: string;
  due_at: string | null;
  reminder_at: string | null;
  created_at: string;
  updated_at: string;
}

interface SubtaskRow {
  id: string;
  task_id: string;
  title: string;
  done: number;
  position: number;
}

interface Subtask {
  id: string;
  task_id: string;
  title: string;
  done: boolean;
  position: number;
}

interface Task {
  id: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  due_at: string | null;
  reminder_at: string | null;
  created_at: string;
  updated_at: string;
  subtasks?: Subtask[];
}

function toSubtask(r: SubtaskRow): Subtask {
  return { id: r.id, task_id: r.task_id, title: r.title, done: r.done === 1, position: r.position };
}

function toTask(r: TaskRow, subtasks?: Subtask[]): Task {
  const task: Task = {
    id: r.id,
    title: r.title,
    description: r.description,
    status: r.status as TaskStatus,
    priority: r.priority as TaskPriority,
    due_at: r.due_at,
    reminder_at: r.reminder_at,
    created_at: r.created_at,
    updated_at: r.updated_at,
  };
  if (subtasks) task.subtasks = subtasks;
  return task;
}

async function getSubtasks(db: D1Database, taskId: string): Promise<Subtask[]> {
  const rows = await all<SubtaskRow>(
    db,
    'SELECT id, task_id, title, done, position FROM subtasks WHERE task_id = ? ORDER BY position ASC',
    taskId
  );
  return rows.map(toSubtask);
}

function validDate(value: unknown): value is string | null {
  return value === null || (typeof value === 'string' && !Number.isNaN(Date.parse(value)));
}

export const taskRoutes = new Hono<Vars>();
taskRoutes.use('*', authMiddleware);

/** GET /api/tasks — list tasks newest first, without subtasks. */
taskRoutes.get('/', async (c) => {
  const userId = c.get('userId');
  const statusParam = c.req.query('status');
  let statusFilter: TaskStatus | undefined;
  if (statusParam === 'todo' || statusParam === 'done') {
    statusFilter = statusParam;
  } else if (statusParam !== undefined) {
    return c.json({ error: 'Invalid status filter' }, 400);
  }
  const rows = statusFilter
    ? await all<TaskRow>(
        c.env.DB,
        'SELECT * FROM tasks WHERE user_id = ? AND status = ? ORDER BY created_at DESC',
        userId,
        statusFilter
      )
    : await all<TaskRow>(c.env.DB, 'SELECT * FROM tasks WHERE user_id = ? ORDER BY created_at DESC', userId);
  return c.json({ tasks: rows.map((r) => toTask(r)) }, 200);
});

/** POST /api/tasks — create a task. */
taskRoutes.post('/', async (c) => {
  const userId = c.get('userId');
  const parsed = await readJson<{
    title?: unknown;
    description?: unknown;
    priority?: unknown;
    due_at?: unknown;
    reminder_at?: unknown;
  }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { title, description, priority, due_at, reminder_at } = parsed.body;

  if (typeof title !== 'string' || title.trim().length === 0) {
    return c.json({ error: 'Title is required' }, 400);
  }
  let prio: TaskPriority = 'medium';
  if (priority !== undefined) {
    if (priority !== 'low' && priority !== 'medium' && priority !== 'high') {
      return c.json({ error: 'Invalid priority' }, 400);
    }
    prio = priority as TaskPriority;
  }
  if (description !== undefined && description !== null && typeof description !== 'string') {
    return c.json({ error: 'Invalid description' }, 400);
  }
  if (due_at !== undefined && !validDate(due_at)) return c.json({ error: 'Invalid due_at' }, 400);
  if (reminder_at !== undefined && !validDate(reminder_at)) return c.json({ error: 'Invalid reminder_at' }, 400);

  const now = nowIso();
  const id = newId();
  await c.env.DB.prepare(
    'INSERT INTO tasks (id, user_id, title, description, status, priority, due_at, reminder_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
  )
    .bind(
      id,
      userId,
      title.trim(),
      typeof description === 'string' ? description : null,
      'todo',
      prio,
      typeof due_at === 'string' ? due_at : null,
      typeof reminder_at === 'string' ? reminder_at : null,
      now,
      now
    )
    .run();
  const row = await first<TaskRow>(c.env.DB, 'SELECT * FROM tasks WHERE id = ?', id);
  if (!row) return c.json({ error: 'Task not found' }, 404);
  return c.json({ task: toTask(row, []) }, 201);
});

/** GET /api/tasks/:id — task detail with subtasks. */
taskRoutes.get('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const row = await first<TaskRow>(c.env.DB, 'SELECT * FROM tasks WHERE id = ? AND user_id = ?', id, userId);
  if (!row) return c.json({ error: 'Task not found' }, 404);
  return c.json({ task: toTask(row, await getSubtasks(c.env.DB, id)) }, 200);
});

/** PUT /api/tasks/:id — partial update. */
taskRoutes.put('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const parsed = await readJson<{
    title?: unknown;
    description?: unknown;
    status?: unknown;
    priority?: unknown;
    due_at?: unknown;
    reminder_at?: unknown;
  }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { title, description, status, priority, due_at, reminder_at } = parsed.body;

  const sets: string[] = [];
  const vals: unknown[] = [];
  if (title !== undefined) {
    if (typeof title !== 'string' || title.trim().length === 0) return c.json({ error: 'Invalid title' }, 400);
    sets.push('title = ?');
    vals.push(title.trim());
  }
  if (description !== undefined) {
    if (description !== null && typeof description !== 'string') return c.json({ error: 'Invalid description' }, 400);
    sets.push('description = ?');
    vals.push(description);
  }
  if (status !== undefined) {
    if (status !== 'todo' && status !== 'done') return c.json({ error: 'Invalid status' }, 400);
    sets.push('status = ?');
    vals.push(status);
  }
  if (priority !== undefined) {
    if (priority !== 'low' && priority !== 'medium' && priority !== 'high') {
      return c.json({ error: 'Invalid priority' }, 400);
    }
    sets.push('priority = ?');
    vals.push(priority);
  }
  if (due_at !== undefined) {
    if (!validDate(due_at)) return c.json({ error: 'Invalid due_at' }, 400);
    sets.push('due_at = ?');
    vals.push(due_at);
  }
  if (reminder_at !== undefined) {
    if (!validDate(reminder_at)) return c.json({ error: 'Invalid reminder_at' }, 400);
    sets.push('reminder_at = ?');
    vals.push(reminder_at);
  }
  if (sets.length === 0) return c.json({ error: 'No fields to update' }, 400);

  sets.push('updated_at = ?');
  vals.push(nowIso(), id, userId);
  const res = await c.env.DB.prepare(`UPDATE tasks SET ${sets.join(', ')} WHERE id = ? AND user_id = ?`)
    .bind(...vals)
    .run();
  if ((res.meta.changes ?? 0) === 0) return c.json({ error: 'Task not found' }, 404);

  const row = await first<TaskRow>(c.env.DB, 'SELECT * FROM tasks WHERE id = ? AND user_id = ?', id, userId);
  if (!row) return c.json({ error: 'Task not found' }, 404);
  return c.json({ task: toTask(row, await getSubtasks(c.env.DB, id)) }, 200);
});

/** DELETE /api/tasks/:id — delete task and its subtasks. */
taskRoutes.delete('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const existing = await first<{ id: string }>(
    c.env.DB,
    'SELECT id FROM tasks WHERE id = ? AND user_id = ?',
    id,
    userId
  );
  if (!existing) return c.json({ error: 'Task not found' }, 404);
  await c.env.DB.batch([
    c.env.DB.prepare('DELETE FROM subtasks WHERE task_id = ?').bind(id),
    c.env.DB.prepare('DELETE FROM tasks WHERE id = ? AND user_id = ?').bind(id, userId),
  ]);
  return c.json({ ok: true }, 200);
});

/** POST /api/tasks/:id/subtasks — create a subtask. */
taskRoutes.post('/:id/subtasks', async (c) => {
  const userId = c.get('userId');
  const taskId = c.req.param('id');
  const task = await first<{ id: string }>(
    c.env.DB,
    'SELECT id FROM tasks WHERE id = ? AND user_id = ?',
    taskId,
    userId
  );
  if (!task) return c.json({ error: 'Task not found' }, 404);

  const parsed = await readJson<{ title?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { title } = parsed.body;
  if (typeof title !== 'string' || title.trim().length === 0) {
    return c.json({ error: 'Title is required' }, 400);
  }

  const maxPos = await first<{ maxPos: number | null }>(
    c.env.DB,
    'SELECT MAX(position) AS maxPos FROM subtasks WHERE task_id = ?',
    taskId
  );
  const id = newId();
  await c.env.DB.prepare('INSERT INTO subtasks (id, task_id, title, done, position) VALUES (?, ?, ?, ?, ?)')
    .bind(id, taskId, title.trim(), 0, (maxPos?.maxPos ?? -1) + 1)
    .run();
  const row = await first<SubtaskRow>(c.env.DB, 'SELECT id, task_id, title, done, position FROM subtasks WHERE id = ?', id);
  if (!row) return c.json({ error: 'Subtask not found' }, 404);
  return c.json({ subtask: toSubtask(row) }, 201);
});

async function findOwnedSubtask(db: D1Database, userId: string, id: string): Promise<SubtaskRow | null> {
  return first<SubtaskRow>(
    db,
    'SELECT s.id, s.task_id, s.title, s.done, s.position FROM subtasks s JOIN tasks t ON t.id = s.task_id WHERE s.id = ? AND t.user_id = ?',
    id,
    userId
  );
}

export const subtaskRoutes = new Hono<Vars>();
subtaskRoutes.use('*', authMiddleware);

/** PUT /api/subtasks/:id — update title and/or done. */
subtaskRoutes.put('/subtasks/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const existing = await findOwnedSubtask(c.env.DB, userId, id);
  if (!existing) return c.json({ error: 'Subtask not found' }, 404);

  const parsed = await readJson<{ title?: unknown; done?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { title, done } = parsed.body;

  const sets: string[] = [];
  const vals: unknown[] = [];
  if (title !== undefined) {
    if (typeof title !== 'string' || title.trim().length === 0) return c.json({ error: 'Invalid title' }, 400);
    sets.push('title = ?');
    vals.push(title.trim());
  }
  if (done !== undefined) {
    if (typeof done !== 'boolean') return c.json({ error: 'Invalid done value' }, 400);
    sets.push('done = ?');
    vals.push(done ? 1 : 0);
  }
  if (sets.length > 0) {
    vals.push(id);
    await c.env.DB.prepare(`UPDATE subtasks SET ${sets.join(', ')} WHERE id = ?`).bind(...vals).run();
  }
  const row = await first<SubtaskRow>(
    c.env.DB,
    'SELECT id, task_id, title, done, position FROM subtasks WHERE id = ?',
    id
  );
  if (!row) return c.json({ error: 'Subtask not found' }, 404);
  return c.json({ subtask: toSubtask(row) }, 200);
});

/** DELETE /api/subtasks/:id */
subtaskRoutes.delete('/subtasks/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const existing = await findOwnedSubtask(c.env.DB, userId, id);
  if (!existing) return c.json({ error: 'Subtask not found' }, 404);
  await c.env.DB.prepare('DELETE FROM subtasks WHERE id = ?').bind(id).run();
  return c.json({ ok: true }, 200);
});
