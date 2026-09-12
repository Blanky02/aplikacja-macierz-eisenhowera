import type { AuthUser, Env } from './types';

type JwtPayload = {
  sub?: string;
  email?: string;
  exp?: number;
  iat?: number;
};

function decodeBase64Url(value: string): Uint8Array {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
  const binary = atob(normalized);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function encodeBase64Url(value: Uint8Array): string {
  let binary = '';
  value.forEach((byte) => { binary += String.fromCharCode(byte); });
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

function encodeJsonPart(value: unknown): string {
  return encodeBase64Url(new TextEncoder().encode(JSON.stringify(value)));
}

function decodeJsonPart<T>(value: string): T {
  return JSON.parse(new TextDecoder().decode(decodeBase64Url(value))) as T;
}

export async function hashToken(value: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
  return encodeBase64Url(new Uint8Array(digest));
}

async function verifyHs256Jwt(token: string, secret: string): Promise<AuthUser> {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('Malformed bearer token');
  const [encodedHeader, encodedPayload, encodedSignature] = parts;
  const header = decodeJsonPart<{ alg?: string }>(encodedHeader);
  const payload = decodeJsonPart<JwtPayload>(encodedPayload);
  if (header.alg !== 'HS256' || !payload.sub || !payload.email) throw new Error('Unsupported bearer token');
  if (payload.exp && payload.exp < Math.floor(Date.now() / 1000)) throw new Error('Bearer token expired');

  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['verify'],
  );
  const valid = await crypto.subtle.verify(
    'HMAC',
    key,
    decodeBase64Url(encodedSignature) as unknown as BufferSource,
    new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`) as unknown as BufferSource,
  );
  if (!valid) throw new Error('Invalid bearer token');
  return { id: payload.sub, email: payload.email };
}

export async function signHs256Jwt(
  user: AuthUser,
  secret: string,
  expiresInSeconds = 60 * 60 * 24 * 30,
): Promise<string> {
  if (!secret) throw new Error('AUTH_JWT_SECRET is not configured');
  const now = Math.floor(Date.now() / 1000);
  const encodedHeader = encodeJsonPart({ alg: 'HS256', typ: 'JWT' });
  const encodedPayload = encodeJsonPart({
    sub: user.id,
    email: user.email,
    iat: now,
    exp: now + expiresInSeconds,
  });
  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign'],
  );
  const signature = await crypto.subtle.sign(
    'HMAC',
    key,
    new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`) as unknown as BufferSource,
  );
  return `${encodedHeader}.${encodedPayload}.${encodeBase64Url(new Uint8Array(signature))}`;
}

export function authResponse(user: AuthUser, accessToken: string) {
  return {
    accessToken,
    tokenType: 'Bearer',
    expiresIn: 60 * 60 * 24 * 30,
    user: { id: user.id, email: user.email },
  };
}

/**
 * Production requests must contain a JWT signed by the configured auth layer.
 * The development header is intentionally disabled outside ENVIRONMENT=development.
 */
export async function authenticate(request: Request, env: Env): Promise<AuthUser> {
  if (env.ENVIRONMENT === 'development') {
    const devUserId = request.headers.get('X-Fokus-Dev-User');
    if (devUserId) {
      return {
        id: devUserId,
        email: request.headers.get('X-Fokus-Dev-Email') || `${devUserId}@local.fokus`,
      };
    }
  }

  const authorization = request.headers.get('Authorization');
  if (!authorization?.startsWith('Bearer ')) throw new Error('Authentication required');
  if (!env.AUTH_JWT_SECRET) throw new Error('AUTH_JWT_SECRET is not configured');
  return verifyHs256Jwt(authorization.slice('Bearer '.length), env.AUTH_JWT_SECRET);
}
