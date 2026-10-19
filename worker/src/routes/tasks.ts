import { Hono } from 'hono';
import type { Env } from '../db';
import { all, first, newId, nowIso, readJson } from '../db';
import { authMiddleware } from '../auth';
import {
  getLabelsForTask,
  parseLabelIds,
  setTaskLabels,
  validateUserLabels,
  type Label,
} from '../labels';

type Vars = { Bindings: Env; Variables: { userId: string } };

type TaskStatus = 'todo' | 'done';
type TaskPriority = 'low' | 'medium' | 'high';
type Recurrence = 'none' | 'daily' | 'weekly' | 'monthly';

interface TaskRow {
  id: string;
  user_id: string;
  title: string;
  description: string | null;
  status: string;
  priority: string;
  due_at: string | null;
  reminder_at: string | null;
  project_id: string | null;
  recurrence: string;
  deleted_at: string | null;
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
  project_id: string | null;
  recurrence: Recurrence;
  deleted_at: string | null;
  created_at: string;
  updated_at: string;
  labels: Label[];
  subtasks?: Subtask[];
}

function toSubtask(r: SubtaskRow): Subtask {
  return { id: r.id, task_id: r.task_id, title: r.title, done: r.done === 1, position: r.position };
}

function toTask(r: TaskRow, subtasks?: Subtask[], labels?: Label[]): Task {
  const task: Task = {
    id: r.id,
    title: r.title,
    description: r.description,
    status: r.status as TaskStatus,
    priority: r.priority as TaskPriority,
    due_at: r.due_at,
    reminder_at: r.reminder_at,
    project_id: r.project_id,
    recurrence: r.recurrence as Recurrence,
    deleted_at: r.deleted_at,
    created_at: r.created_at,
    updated_at: r.updated_at,
    labels: labels ?? [],
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

/** Full task detail: row + subtasks + labels. */
async function getTaskDetail(db: D1Database, id: string): Promise<Task | null> {
  const row = await first<TaskRow>(db, 'SELECT * FROM tasks WHERE id = ?', id);
  if (!row) return null;
  return toTask(row, await getSubtasks(db, id), await getLabelsForTask(db, id));
}

function validDate(value: unknown): value is string | null {
  return value === null || (typeof value === 'string' && !Number.isNaN(Date.parse(value)));
}

function validRecurrence(value: unknown): value is Recurrence {
  return value === 'none' || value === 'daily' || value === 'weekly' || value === 'monthly';
}

/** Validate an optional project_id field: must be null or a project owned by the user. */
async function resolveProjectId(
  db: D1Database,
  userId: string,
  projectId: unknown
): Promise<{ ok: true; value: string | null } | { ok: false }> {
  if (projectId === null) return { ok: true, value: null };
  if (typeof projectId !== 'string') return { ok: false };
  const row = await first<{ id: string }>(
    db,
    'SELECT id FROM projects WHERE id = ? AND user_id = ?',
    projectId,
    userId
  );
  if (!row) return { ok: false };
  return { ok: true, value: projectId };
}

/** Shift an ISO date forward according to a recurrence rule. */
function shiftDate(iso: string, recurrence: Recurrence): string {
  const d = new Date(iso);
  if (recurrence === 'daily') d.setUTCDate(d.getUTCDate() + 1);
  else if (recurrence === 'weekly') d.setUTCDate(d.getUTCDate() + 7);
  else if (recurrence === 'monthly') d.setUTCMonth(d.getUTCMonth() + 1);
  return d.toISOString();
}

/**
 * Clone a completed recurring task into its next occurrence:
 * new id, status todo, dates shifted, labels copied, subtasks copied unchecked.
 */
async function spawnNextOccurrence(db: D1Database, userId: string, row: TaskRow): Promise<Task> {
  const recurrence = row.recurrence as Recurrence;
  const now = nowIso();
  const id = newId();
  await db
    .prepare(
      `INSERT INTO tasks (id, user_id, title, description, status, priority, due_at, reminder_at,
        project_id, recurrence, created_at, updated_at)
       VALUES (?, ?, ?, ?, 'todo', ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      id,
      userId,
      row.title,
      row.description,
      row.priority,
      row.due_at ? shiftDate(row.due_at, recurrence) : shiftDate(now, recurrence),
      row.reminder_at ? shiftDate(row.reminder_at, recurrence) : null,
      row.project_id,
      recurrence,
      now,
      now
    )
    .run();
  const labels = await getLabelsForTask(db, row.id);
  await setTaskLabels(
    db,
    id,
    labels.map((l) => l.id)
  );
  const subs = await getSubtasks(db, row.id);
  for (const s of subs) {
    await db
      .prepare('INSERT INTO subtasks (id, task_id, title, done, position) VALUES (?, ?, ?, 0, ?)')
      .bind(newId(), id, s.title, s.position)
      .run();
  }
  const detail = await getTaskDetail(db, id);
  if (!detail) throw new Error('Failed to load cloned task');
  return detail;
}

export const taskRoutes = new Hono<Vars>();
taskRoutes.use('*', authMiddleware);

/** GET /api/tasks — list tasks newest first, without subtasks. Deleted tasks excluded unless ?trashed=true. */
taskRoutes.get('/', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;

  const statusParam = c.req.query('status');
  let statusFilter: TaskStatus | undefined;
  if (statusParam === 'todo' || statusParam === 'done') {
    statusFilter = statusParam;
  } else if (statusParam !== undefined) {
    return c.json({ error: 'Invalid status filter' }, 400);
  }

  const trashed = c.req.query('trashed') === 'true';

  const projectParam = c.req.query('project_id');
  if (projectParam !== undefined) {
    const resolved = await resolveProjectId(db, userId, projectParam);
    if (!resolved.ok) return c.json({ error: 'Invalid project_id filter' }, 400);
  }

  const labelParam = c.req.query('label_id');
  if (labelParam !== undefined) {
    const valid = await validateUserLabels(db, userId, [labelParam]);
    if (!valid) return c.json({ error: 'Invalid label_id filter' }, 400);
  }

  let sql = 'SELECT * FROM tasks WHERE user_id = ?';
  const params: unknown[] = [userId];
  if (trashed) {
    sql += ' AND deleted_at IS NOT NULL';
  } else {
    sql += ' AND deleted_at IS NULL';
  }
  if (statusFilter) {
    sql += ' AND status = ?';
    params.push(statusFilter);
  }
  if (projectParam !== undefined) {
    sql += ' AND project_id = ?';
    params.push(projectParam);
  }
  if (labelParam !== undefined) {
    sql += ' AND EXISTS (SELECT 1 FROM task_labels tl WHERE tl.task_id = tasks.id AND tl.label_id = ?)';
    params.push(labelParam);
  }
  sql += trashed ? ' ORDER BY deleted_at DESC' : ' ORDER BY created_at DESC';

  const rows = await all<TaskRow>(db, sql, ...params);
  const tasks: Task[] = [];
  for (const r of rows) {
    tasks.push(toTask(r, undefined, await getLabelsForTask(db, r.id)));
  }
  return c.json({ tasks }, 200);
});

/** POST /api/tasks — create a task. */
taskRoutes.post('/', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;
  const parsed = await readJson<{
    title?: unknown;
    description?: unknown;
    priority?: unknown;
    due_at?: unknown;
    reminder_at?: unknown;
    project_id?: unknown;
    recurrence?: unknown;
    label_ids?: unknown;
  }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { title, description, priority, due_at, reminder_at, project_id, recurrence, label_ids } = parsed.body;

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

  let projectIdVal: string | null = null;
  if (project_id !== undefined) {
    const resolved = await resolveProjectId(db, userId, project_id);
    if (!resolved.ok) return c.json({ error: 'Invalid project_id' }, 400);
    projectIdVal = resolved.value;
  }

  let recur: Recurrence = 'none';
  if (recurrence !== undefined) {
    if (!validRecurrence(recurrence)) return c.json({ error: 'Invalid recurrence' }, 400);
    recur = recurrence;
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
      `INSERT INTO tasks (id, user_id, title, description, status, priority, due_at, reminder_at,
        project_id, recurrence, created_at, updated_at)
       VALUES (?, ?, ?, ?, 'todo', ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      id,
      userId,
      title.trim(),
      typeof description === 'string' ? description : null,
      prio,
      typeof due_at === 'string' ? due_at : null,
      typeof reminder_at === 'string' ? reminder_at : null,
      projectIdVal,
      recur,
      now,
      now
    )
    .run();
  await setTaskLabels(db, id, labelIdVals);
  const task = await getTaskDetail(db, id);
  if (!task) return c.json({ error: 'Task not found' }, 404);
  task.subtasks = [];
  return c.json({ task }, 201);
});

/** GET /api/tasks/:id — task detail with subtasks and labels. */
taskRoutes.get('/:id', async (c) => {
  const userId = c.get('userId');
  const id = c.req.param('id');
  const row = await first<TaskRow>(c.env.DB, 'SELECT * FROM tasks WHERE id = ? AND user_id = ?', id, userId);
  if (!row) return c.json({ error: 'Task not found' }, 404);
  const task = await getTaskDetail(c.env.DB, id);
  if (!task) return c.json({ error: 'Task not found' }, 404);
  return c.json({ task }, 200);
});

/** PUT /api/tasks/:id — partial update. Completing a recurring task spawns its next occurrence. */
taskRoutes.put('/:id', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;
  const id = c.req.param('id');
  const parsed = await readJson<{
    title?: unknown;
    description?: unknown;
    status?: unknown;
    priority?: unknown;
    due_at?: unknown;
    reminder_at?: unknown;
    project_id?: unknown;
    recurrence?: unknown;
    label_ids?: unknown;
  }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { title, description, status, priority, due_at, reminder_at, project_id, recurrence, label_ids } =
    parsed.body;

  const existing = await first<TaskRow>(db, 'SELECT * FROM tasks WHERE id = ? AND user_id = ?', id, userId);
  if (!existing) return c.json({ error: 'Task not found' }, 404);

  // Validate everything before touching the database.
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
  if (project_id !== undefined) {
    const resolved = await resolveProjectId(db, userId, project_id);
    if (!resolved.ok) return c.json({ error: 'Invalid project_id' }, 400);
    sets.push('project_id = ?');
    vals.push(resolved.value);
  }
  if (recurrence !== undefined) {
    if (!validRecurrence(recurrence)) return c.json({ error: 'Invalid recurrence' }, 400);
    sets.push('recurrence = ?');
    vals.push(recurrence);
  }
  if (sets.length === 0 && labelIdVals === undefined) return c.json({ error: 'No fields to update' }, 400);

  const prevStatus = existing.status;
  if (sets.length > 0) {
    sets.push('updated_at = ?');
    vals.push(nowIso(), id, userId);
    const res = await db.prepare(`UPDATE tasks SET ${sets.join(', ')} WHERE id = ? AND user_id = ?`)
      .bind(...vals)
      .run();
    if ((res.meta.changes ?? 0) === 0) return c.json({ error: 'Task not found' }, 404);
  }
  if (labelIdVals !== undefined) {
    await setTaskLabels(db, id, labelIdVals);
  }

  const updated = await first<TaskRow>(db, 'SELECT * FROM tasks WHERE id = ? AND user_id = ?', id, userId);
  if (!updated) return c.json({ error: 'Task not found' }, 404);
  const task = await getTaskDetail(db, id);
  if (!task) return c.json({ error: 'Task not found' }, 404);

  // Recurrence: completing a recurring task spawns its next occurrence.
  if (status === 'done' && prevStatus !== 'done' && (updated.recurrence as Recurrence) !== 'none') {
    const nextTask = await spawnNextOccurrence(db, userId, updated);
    return c.json({ task, next_task: nextTask }, 200);
  }
  return c.json({ task }, 200);
});

/** DELETE /api/tasks/:id — soft delete unless ?permanent=true. */
taskRoutes.delete('/:id', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;
  const id = c.req.param('id');
  const existing = await first<{ id: string }>(
    db,
    'SELECT id FROM tasks WHERE id = ? AND user_id = ?',
    id,
    userId
  );
  if (!existing) return c.json({ error: 'Task not found' }, 404);

  if (c.req.query('permanent') === 'true') {
    await db.batch([
      db.prepare('DELETE FROM subtasks WHERE task_id = ?').bind(id),
      db.prepare('DELETE FROM task_labels WHERE task_id = ?').bind(id),
      db.prepare('DELETE FROM tasks WHERE id = ? AND user_id = ?').bind(id, userId),
    ]);
  } else {
    const now = nowIso();
    await db
      .prepare('UPDATE tasks SET deleted_at = ?, updated_at = ? WHERE id = ? AND user_id = ?')
      .bind(now, now, id, userId)
      .run();
  }
  return c.json({ ok: true }, 200);
});

/** POST /api/tasks/:id/restore — un-delete a soft-deleted task. */
taskRoutes.post('/:id/restore', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;
  const id = c.req.param('id');
  const res = await db
    .prepare('UPDATE tasks SET deleted_at = NULL, updated_at = ? WHERE id = ? AND user_id = ? AND deleted_at IS NOT NULL')
    .bind(nowIso(), id, userId)
    .run();
  if ((res.meta.changes ?? 0) === 0) return c.json({ error: 'Task not found' }, 404);
  const task = await getTaskDetail(db, id);
  if (!task) return c.json({ error: 'Task not found' }, 404);
  return c.json({ task }, 200);
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
