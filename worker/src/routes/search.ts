import { Hono } from 'hono';
import type { Env } from '../db';
import { all, first } from '../db';
import { authMiddleware } from '../auth';
import { getLabelsForNote, getLabelsForTask, type Label } from '../labels';

type Vars = { Bindings: Env; Variables: { userId: string } };

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

/** Escape LIKE wildcards so the query is matched literally. */
function escapeLike(q: string): string {
  return q.replace(/\\/g, '\\\\').replace(/%/g, '\\%').replace(/_/g, '\\_');
}

async function serializeTask(db: D1Database, r: TaskRow): Promise<Record<string, unknown>> {
  const subRows = await all<SubtaskRow>(
    db,
    'SELECT id, task_id, title, done, position FROM subtasks WHERE task_id = ? ORDER BY position ASC',
    r.id
  );
  const labels: Label[] = await getLabelsForTask(db, r.id);
  return {
    id: r.id,
    title: r.title,
    description: r.description,
    status: r.status,
    priority: r.priority,
    due_at: r.due_at,
    reminder_at: r.reminder_at,
    project_id: r.project_id,
    recurrence: r.recurrence,
    deleted_at: r.deleted_at,
    created_at: r.created_at,
    updated_at: r.updated_at,
    labels,
    subtasks: subRows.map((s) => ({
      id: s.id,
      task_id: s.task_id,
      title: s.title,
      done: s.done === 1,
      position: s.position,
    })),
  };
}

async function serializeNote(db: D1Database, r: NoteRow): Promise<Record<string, unknown>> {
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
    labels: await getLabelsForNote(db, r.id),
  };
}

export const searchRoutes = new Hono<Vars>();
searchRoutes.use('*', authMiddleware);

/** GET /api/search?q= — universal search across tasks (incl. subtask titles), notes. */
searchRoutes.get('/', async (c) => {
  const userId = c.get('userId');
  const db = c.env.DB;
  const q = (c.req.query('q') ?? '').trim();
  if (q.length < 2) {
    return c.json({ error: 'Query must be at least 2 characters' }, 400);
  }
  const like = `%${escapeLike(q)}%`;

  // Tasks matching title or description.
  const directTasks = await all<TaskRow>(
    db,
    `SELECT * FROM tasks WHERE user_id = ? AND deleted_at IS NULL
       AND (title LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\')
     ORDER BY updated_at DESC LIMIT 20`,
    userId,
    like,
    like
  );

  // Tasks whose subtasks match the query — include the parent task.
  const parentIds = await all<{ task_id: string }>(
    db,
    `SELECT DISTINCT s.task_id AS task_id FROM subtasks s
     JOIN tasks t ON t.id = s.task_id
     WHERE t.user_id = ? AND t.deleted_at IS NULL AND s.title LIKE ? ESCAPE '\\'`,
    userId,
    like
  );
  const seen = new Set(directTasks.map((t) => t.id));
  const extraIds = parentIds.map((p) => p.task_id).filter((id) => !seen.has(id)).slice(0, 20);
  const extraTasks: TaskRow[] = [];
  for (const id of extraIds) {
    const row = await first<TaskRow>(db, 'SELECT * FROM tasks WHERE id = ? AND user_id = ?', id, userId);
    if (row) extraTasks.push(row);
  }

  const tasks: Record<string, unknown>[] = [];
  for (const r of [...directTasks, ...extraTasks]) {
    tasks.push(await serializeTask(db, r));
  }

  // Notes matching title or content.
  const noteRows = await all<NoteRow>(
    db,
    `SELECT * FROM notes WHERE user_id = ? AND deleted_at IS NULL
       AND (title LIKE ? ESCAPE '\\' OR content LIKE ? ESCAPE '\\')
     ORDER BY updated_at DESC LIMIT 20`,
    userId,
    like,
    like
  );
  const notes: Record<string, unknown>[] = [];
  for (const r of noteRows) {
    notes.push(await serializeNote(db, r));
  }

  return c.json({ tasks, notes }, 200);
});
