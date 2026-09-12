import { describe, expect, it } from 'vitest';
import { verifySaltEdgeCallback } from '../src/signature';

function toBase64(bytes: ArrayBuffer): string {
  return btoa(String.fromCharCode(...new Uint8Array(bytes)));
}

function toPem(bytes: ArrayBuffer): string {
  const base64 = toBase64(bytes);
  const lines = base64.match(/.{1,64}/g)?.join('\n') || '';
  return `-----BEGIN PUBLIC KEY-----\n${lines}\n-----END PUBLIC KEY-----`;
}

describe('Salt Edge callback signature verification', () => {
  it('accepts a callback signed with the configured RSA public key', async () => {
    const pair = await crypto.subtle.generateKey(
      { name: 'RSASSA-PKCS1-v1_5', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' },
      true,
      ['sign', 'verify'],
    );
    const url = 'https://api.example.test/api/bank/callback';
    const body = JSON.stringify({ data: { customer_id: 'customer-1' } });
    const signature = await crypto.subtle.sign(
      'RSASSA-PKCS1-v1_5',
      pair.privateKey,
      new TextEncoder().encode(`${url}|${body}`),
    );
    const publicKey = toPem(await crypto.subtle.exportKey('spki', pair.publicKey));

    await expect(verifySaltEdgeCallback(url, body, toBase64(signature), publicKey)).resolves.toBe(true);
    await expect(verifySaltEdgeCallback(url, `${body}x`, toBase64(signature), publicKey)).resolves.toBe(false);
  });
});
