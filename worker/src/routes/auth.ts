import { Hono } from 'hono';
import type { Env } from '../db';
import { first, newId, nowIso, readJson } from '../db';
import { authMiddleware, checkRateLimit, generateSessionToken, hashPassword, sessionExpiryIso, verifyPassword } from '../auth';

interface UserRow {
  id: string;
  email: string;
  name: string;
  password_hash?: string;
  created_at: string;
}

interface PublicUser {
  id: string;
  email: string;
  name: string;
  created_at: string;
}

function toPublicUser(row: UserRow): PublicUser {
  return { id: row.id, email: row.email, name: row.name, created_at: row.created_at };
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

async function createSession(db: D1Database, userId: string): Promise<string> {
  const token = generateSessionToken();
  const now = nowIso();
  await db
    .prepare('INSERT INTO sessions (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)')
    .bind(token, userId, now, sessionExpiryIso())
    .run();
  return token;
}

export const publicAuthRoutes = new Hono<{ Bindings: Env }>();

publicAuthRoutes.post('/register', async (c) => {
  const parsed = await readJson<{ email?: unknown; password?: unknown; name?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { email, password, name } = parsed.body;

  if (typeof email !== 'string' || !EMAIL_RE.test(email.trim())) {
    return c.json({ error: 'Invalid email address' }, 400);
  }
  if (typeof password !== 'string' || password.length < 6 || password.length > 128) {
    return c.json({ error: 'Password must be between 6 and 128 characters' }, 400);
  }
  if (typeof name !== 'string' || name.trim().length === 0) {
    return c.json({ error: 'Name is required' }, 400);
  }

  const ip = c.req.header('CF-Connecting-IP') ?? 'unknown';
  if (!(await checkRateLimit(c.env.DB, `register:${ip}`, 10, 900))) {
    return c.json({ error: 'Too many attempts, try again later' }, 429);
  }

  const normalizedEmail = email.trim().toLowerCase();
  const existing = await first<{ id: string }>(
    c.env.DB,
    'SELECT id FROM users WHERE email = ?',
    normalizedEmail
  );
  if (existing) return c.json({ error: 'Email already registered' }, 409);

  const id = newId();
  const createdAt = nowIso();
  const passwordHash = await hashPassword(password);
  await c.env.DB.prepare('INSERT INTO users (id, email, name, password_hash, created_at) VALUES (?, ?, ?, ?, ?)')
    .bind(id, normalizedEmail, name.trim(), passwordHash, createdAt)
    .run();

  const token = await createSession(c.env.DB, id);
  return c.json({ token, user: toPublicUser({ id, email: normalizedEmail, name: name.trim(), created_at: createdAt }) }, 201);
});

publicAuthRoutes.post('/login', async (c) => {
  const parsed = await readJson<{ email?: unknown; password?: unknown }>(c);
  if (!parsed.ok) return c.json({ error: 'Invalid JSON body' }, 400);
  const { email, password } = parsed.body;

  if (typeof email !== 'string' || typeof password !== 'string' || password.length > 128) {
    return c.json({ error: 'Invalid email or password' }, 401);
  }
  const ip = c.req.header('CF-Connecting-IP') ?? 'unknown';
  if (!(await checkRateLimit(c.env.DB, `login:${ip}`, 20, 900))) {
    return c.json({ error: 'Too many attempts, try again later' }, 429);
  }
  const row = await first<UserRow>(
    c.env.DB,
    'SELECT id, email, name, password_hash, created_at FROM users WHERE email = ?',
    email.trim().toLowerCase()
  );
  if (!row || !row.password_hash || !(await verifyPassword(password, row.password_hash))) {
    return c.json({ error: 'Invalid email or password' }, 401);
  }
  const token = await createSession(c.env.DB, row.id);
  return c.json({ token, user: toPublicUser(row) }, 200);
});

export const protectedAuthRoutes = new Hono<{ Bindings: Env; Variables: { userId: string } }>();
protectedAuthRoutes.use('*', authMiddleware);

protectedAuthRoutes.post('/logout', async (c) => {
  const header = c.req.header('Authorization') ?? '';
  const token = header.slice('Bearer '.length).trim();
  await c.env.DB.prepare('DELETE FROM sessions WHERE token = ?').bind(token).run();
  return c.json({ ok: true }, 200);
});

protectedAuthRoutes.get('/me', async (c) => {
  const userId = c.get('userId');
  const row = await first<UserRow>(
    c.env.DB,
    'SELECT id, email, name, created_at FROM users WHERE id = ?',
    userId
  );
  if (!row) return c.json({ error: 'User not found' }, 404);
  return c.json({ user: toPublicUser(row) }, 200);
});
