import { describe, expect, it } from 'vitest';
import { authenticate, signHs256Jwt } from '../src/auth';
import type { Env } from '../src/types';

describe('access tokens', () => {
  it('issues and verifies an application JWT', async () => {
    const user = { id: 'user-1', email: 'user@example.test' };
    const secret = 'test-secret';
    const token = await signHs256Jwt(user, secret, 60);
    const request = new Request('https://api.example.test/api/bank/connections', {
      headers: { Authorization: `Bearer ${token}` },
    });
    const env = { AUTH_JWT_SECRET: secret, ENVIRONMENT: 'production' } as Env;
    await expect(authenticate(request, env)).resolves.toEqual(user);
  });

  it('rejects a token signed with another secret', async () => {
    const token = await signHs256Jwt({ id: 'user-1', email: 'user@example.test' }, 'one', 60);
    const request = new Request('https://api.example.test/api/bank/connections', {
      headers: { Authorization: `Bearer ${token}` },
    });
    const env = { AUTH_JWT_SECRET: 'two', ENVIRONMENT: 'production' } as Env;
    await expect(authenticate(request, env)).rejects.toThrow('Invalid bearer token');
  });
});
