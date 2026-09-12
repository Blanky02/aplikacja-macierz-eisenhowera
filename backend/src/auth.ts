import type { AuthUser, Env } from './types';

function decodeBase64Url(value: string): Uint8Array {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
  const binary = atob(normalized);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function decodeJsonPart<T>(value: string): T {
  return JSON.parse(new TextDecoder().decode(decodeBase64Url(value))) as T;
}

async function verifyHs256Jwt(token: string, secret: string): Promise<AuthUser> {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('Malformed bearer token');
  const [encodedHeader, encodedPayload, encodedSignature] = parts;
  const header = decodeJsonPart<{ alg?: string }>(encodedHeader);
  const payload = decodeJsonPart<{ sub?: string; email?: string; exp?: number }>(encodedPayload);
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
    new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`),
  );
  if (!valid) throw new Error('Invalid bearer token');
  return { id: payload.sub, email: payload.email };
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
