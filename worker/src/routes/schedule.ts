import { Hono } from 'hono';
import type { Env } from '../db';
import { all } from '../db';
import { authMiddleware } from '../auth';

type Vars = { Bindings: Env; Variables: { userId: string } };

interface UpcomingRow {
  id: string;
  title: string;
  due_at: string | null;
  reminder_at: string | null;
  priority: string;
}

export const scheduleRoutes = new Hono<Vars>();
scheduleRoutes.use('*', authMiddleware);

/** GET /api/schedule/upcoming — todo tasks with due_at or reminder_at inside the window, sorted by date. */
scheduleRoutes.get('/upcoming', async (c) => {
  const userId = c.get('userId');
  const rawDays = c.req.query('days');
  let days = 7;
  if (rawDays !== undefined) {
    const n = parseInt(rawDays, 10);
    if (!Number.isNaN(n) && n > 0) days = Math.min(30, n);
  }

  const now = Date.now();
  const startIso = new Date(now).toISOString();
  const endIso = new Date(now + days * 24 * 60 * 60 * 1000).toISOString();

  const rows = await all<UpcomingRow>(
    c.env.DB,
    `SELECT id, title, due_at, reminder_at, priority FROM tasks
     WHERE user_id = ? AND status = 'todo' AND deleted_at IS NULL
       AND ((due_at IS NOT NULL AND due_at >= ? AND due_at <= ?)
         OR (reminder_at IS NOT NULL AND reminder_at >= ? AND reminder_at <= ?))`,
    userId,
    startIso,
    endIso,
    startIso,
    endIso
  );

  const items = rows
    .map((r) => {
      const dates = [r.due_at, r.reminder_at].filter((d): d is string => d !== null);
      const sortKey = dates.length > 0 ? [...dates].sort()[0] : '';
      return {
        kind: 'task' as const,
        id: r.id,
        title: r.title,
        due_at: r.due_at,
        reminder_at: r.reminder_at,
        priority: r.priority,
        sortKey,
      };
    })
    .sort((a, b) => (a.sortKey < b.sortKey ? -1 : a.sortKey > b.sortKey ? 1 : 0))
    .map(({ sortKey: _sortKey, ...rest }) => rest);

  return c.json({ items }, 200);
});
