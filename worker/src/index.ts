import { Hono } from 'hono';
import { cors } from 'hono/cors';
import type { Env } from './db';
import { protectedAuthRoutes, publicAuthRoutes } from './routes/auth';
import { subtaskRoutes, taskRoutes } from './routes/tasks';
import { notesRoutes } from './routes/notes';
import { scheduleRoutes } from './routes/schedule';

const app = new Hono<{ Bindings: Env }>();

app.use(
  '*',
  cors({
    origin: '*',
    allowHeaders: ['Content-Type', 'Authorization'],
    allowMethods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    maxAge: 86400,
  })
);

app.route('/api/auth', publicAuthRoutes);
app.route('/api/auth', protectedAuthRoutes);
app.route('/api/tasks', taskRoutes);
app.route('/api', subtaskRoutes);
app.route('/api/notes', notesRoutes);
app.route('/api/schedule', scheduleRoutes);

app.notFound((c) => c.json({ error: 'Not found' }, 404));

app.onError((err, c) => {
  console.error('Unhandled error:', err);
  return c.json({ error: 'Internal server error' }, 500);
});

export default app;
