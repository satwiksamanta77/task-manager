import type { MiddlewareHandler } from 'hono';
import type { Env } from './db';

const PBKDF2_ITERATIONS = 100_000;
const SALT_BYTES = 16;
const TOKEN_BYTES = 32;
const SESSION_DAYS = 30;

function toHex(bytes: Uint8Array): string {
  let out = '';
  for (const b of bytes) out += b.toString(16).padStart(2, '0');
  return out;
}

function fromHex(hex: string): Uint8Array {
  const out = new Uint8Array(hex.length / 2);
  for (let i = 0; i < out.length; i++) {
    out[i] = parseInt(hex.slice(i * 2, i * 2 + 2), 16);
  }
  return out;
}

async function deriveKey(password: string, salt: Uint8Array): Promise<Uint8Array> {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, [
    'deriveBits',
  ]);
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt, iterations: PBKDF2_ITERATIONS, hash: 'SHA-256' },
    key,
    256
  );
  return new Uint8Array(bits);
}

/** Hash a password, stored as "salt:hash" in hex. */
export async function hashPassword(password: string): Promise<string> {
  const salt = crypto.getRandomValues(new Uint8Array(SALT_BYTES));
  const hash = await deriveKey(password, salt);
  return `${toHex(salt)}:${toHex(hash)}`;
}

/** Verify a password against a stored "salt:hash" value, in constant time. */
export async function verifyPassword(password: string, stored: string): Promise<boolean> {
  const parts = stored.split(':');
  if (parts.length !== 2 || !parts[0] || !parts[1]) return false;
  const salt = fromHex(parts[0]);
  const expected = parts[1];
  const actual = toHex(await deriveKey(password, salt));
  if (actual.length !== expected.length) return false;
  let diff = 0;
  for (let i = 0; i < actual.length; i++) {
    diff |= actual.charCodeAt(i) ^ expected.charCodeAt(i);
  }
  return diff === 0;
}

/** Generate a 64-char hex session token. */
export function generateSessionToken(): string {
  return toHex(crypto.getRandomValues(new Uint8Array(TOKEN_BYTES)));
}

/** ISO-8601 UTC expiry for a new session (30 days). */
export function sessionExpiryIso(): string {
  return new Date(Date.now() + SESSION_DAYS * 24 * 60 * 60 * 1000).toISOString();
}

/** Require a valid, unexpired Bearer session token. Sets `userId` on the context. */
export const authMiddleware: MiddlewareHandler<{ Bindings: Env; Variables: { userId: string } }> = async (
  c,
  next
) => {
  const header = c.req.header('Authorization');
  if (!header || !header.startsWith('Bearer ')) {
    return c.json({ error: 'Missing authorization token' }, 401);
  }
  const token = header.slice('Bearer '.length).trim();
  if (!token) {
    return c.json({ error: 'Missing authorization token' }, 401);
  }
  const session = await c.env.DB.prepare('SELECT user_id, expires_at FROM sessions WHERE token = ?')
    .bind(token)
    .first<{ user_id: string; expires_at: string }>();
  if (!session) {
    return c.json({ error: 'Invalid session' }, 401);
  }
  if (Number.isNaN(Date.parse(session.expires_at)) || Date.parse(session.expires_at) <= Date.now()) {
    await c.env.DB.prepare('DELETE FROM sessions WHERE token = ?').bind(token).run();
    return c.json({ error: 'Session expired' }, 401);
  }
  c.set('userId', session.user_id);
  await next();
};
