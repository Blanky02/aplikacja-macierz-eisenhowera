import { createRemoteJWKSet, jwtVerify } from 'jose';
import { Hono } from 'hono';
import {
  authResponse,
  hashToken,
  signHs256Jwt,
} from './auth';
import {
  ensureUser,
  getUserByEmail,
  getUserById,
} from './db';
import type { AuthUser, Env } from './types';

const googleKeys = createRemoteJWKSet(new URL('https://www.googleapis.com/oauth2/v3/certs'));
const auth = new Hono<{ Bindings: Env }>();
const EMAIL_TOKEN_TTL_MS = 15 * 60 * 1000;
const EMAIL_CODE_TTL_MS = 2 * 60 * 1000;

function normalizeEmail(value: unknown): string {
  const email = String(value || '').trim().toLocaleLowerCase('en-US');
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 320) {
    throw new Error('Invalid email address');
  }
  return email;
}

function randomToken(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  let binary = '';
  bytes.forEach((byte) => { binary += String.fromCharCode(byte); });
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

function isoAfter(milliseconds: number): string {
  return new Date(Date.now() + milliseconds).toISOString();
}

function authUserForEmail(email: string, existing: AuthUser | null): AuthUser {
  return existing || { id: `email:${crypto.randomUUID()}`, email };
}

async function issueAccessToken(env: Env, user: AuthUser): Promise<Response> {
  const accessToken = await signHs256Jwt(user, env.AUTH_JWT_SECRET);
  return Response.json(authResponse(user, accessToken));
}

async function sendMagicLink(env: Env, email: string, verificationUrl: string): Promise<boolean> {
  if (!env.RESEND_API_KEY || !env.EMAIL_FROM) {
    if (env.ENVIRONMENT === 'development') return false;
    throw new Error('Email delivery is not configured');
  }
  const response = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${env.RESEND_API_KEY}`,
    },
    body: JSON.stringify({
      from: env.EMAIL_FROM,
      to: [email],
      subject: 'Zaloguj się do Fokus',
      html: `<p>Otwórz poniższy link, aby zalogować się do Fokus:</p><p><a href="${verificationUrl}">Zaloguj się do Fokus</a></p><p>Link wygaśnie za 15 minut.</p>`,
    }),
  });
  if (!response.ok) throw new Error('Email delivery failed');
  return true;
}

auth.post('/google', async (c) => {
  if (!c.env.GOOGLE_WEB_CLIENT_ID) return c.json({ error: 'google_auth_not_configured' }, 503);
  const body = await c.req.json<{ idToken?: string }>().catch(() => ({} as { idToken?: string }));
  if (!body.idToken) return c.json({ error: 'google_id_token_missing' }, 400);

  try {
    const verified = await jwtVerify(body.idToken, googleKeys, {
      issuer: ['https://accounts.google.com', 'accounts.google.com'],
      audience: c.env.GOOGLE_WEB_CLIENT_ID,
    });
    const email = normalizeEmail(verified.payload.email);
    if (verified.payload.email_verified !== true && verified.payload.email_verified !== 'true') {
      return c.json({ error: 'google_email_not_verified' }, 403);
    }
    const existing = await getUserByEmail(c.env.DB, email);
    const user = authUserForEmail(email, existing);
    await ensureUser(c.env.DB, user);
    return issueAccessToken(c.env, user);
  } catch (error) {
    console.error(error);
    return c.json({ error: 'google_auth_failed' }, 401);
  }
});

auth.post('/email/request', async (c) => {
  const body = await c.req.json<{ email?: string }>().catch(() => ({} as { email?: string }));
  let email: string;
  try {
    email = normalizeEmail(body.email);
  } catch {
    return c.json({ error: 'email_invalid' }, 400);
  }

  const rawToken = randomToken();
  const tokenHash = await hashToken(rawToken);
  await c.env.DB.prepare(
    `INSERT INTO auth_email_tokens (id, email, token_hash, expires_at)
     VALUES (?, ?, ?, ?)`,
  ).bind(crypto.randomUUID(), email, tokenHash, isoAfter(EMAIL_TOKEN_TTL_MS)).run();

  const origin = (c.env.PUBLIC_BASE_URL || new URL(c.req.url).origin).replace(/\/$/, '');
  const verificationUrl = `${origin}/api/auth/email/verify?token=${encodeURIComponent(rawToken)}`;
  const delivered = await sendMagicLink(c.env, email, verificationUrl);
  return c.json({
    accepted: true,
    expiresIn: EMAIL_TOKEN_TTL_MS / 1000,
    ...(delivered ? {} : { devVerificationUrl: verificationUrl }),
  });
});

auth.get('/email/verify', async (c) => {
  const rawToken = c.req.query('token');
  if (!rawToken) return c.text('Brak tokenu logowania.', 400);
  const tokenHash = await hashToken(rawToken);
  const now = new Date().toISOString();
  const token = await c.env.DB.prepare(
    `SELECT id, email FROM auth_email_tokens
     WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?`,
  ).bind(tokenHash, now).first<{ id: string; email: string }>();
  if (!token) return c.text('Link logowania wygasł lub został już użyty.', 410);

  const existing = await getUserByEmail(c.env.DB, token.email);
  const user = authUserForEmail(token.email, existing);
  await ensureUser(c.env.DB, user);
  const rawCode = randomToken();
  await c.env.DB.batch([
    c.env.DB.prepare(
      `UPDATE auth_email_tokens SET used_at = ? WHERE id = ? AND used_at IS NULL`,
    ).bind(now, token.id),
    c.env.DB.prepare(
      `INSERT INTO auth_exchange_codes (code_hash, user_id, expires_at) VALUES (?, ?, ?)`,
    ).bind(await hashToken(rawCode), user.id, isoAfter(EMAIL_CODE_TTL_MS)),
  ]);

  const redirect = new URL(c.env.EMAIL_APP_REDIRECT_URI || 'fokus://auth/email');
  redirect.searchParams.set('code', rawCode);
  return Response.redirect(redirect.toString(), 302);
});

auth.post('/email/exchange', async (c) => {
  const body = await c.req.json<{ code?: string }>().catch(() => ({} as { code?: string }));
  if (!body.code) return c.json({ error: 'email_code_missing' }, 400);
  const codeHash = await hashToken(body.code);
  const now = new Date().toISOString();
  const exchange = await c.env.DB.prepare(
    `SELECT code_hash, user_id FROM auth_exchange_codes
     WHERE code_hash = ? AND used_at IS NULL AND expires_at > ?`,
  ).bind(codeHash, now).first<{ code_hash: string; user_id: string }>();
  if (!exchange) return c.json({ error: 'email_code_invalid_or_expired' }, 401);

  const consumed = await c.env.DB.prepare(
    `UPDATE auth_exchange_codes SET used_at = ? WHERE code_hash = ? AND used_at IS NULL`,
  ).bind(now, exchange.code_hash).run();
  if (consumed.meta.changes !== 1) return c.json({ error: 'email_code_already_used' }, 401);
  const user = await getUserById(c.env.DB, exchange.user_id);
  if (!user) return c.json({ error: 'user_not_found' }, 401);
  return issueAccessToken(c.env, user);
});

export { auth as authRoutes };
