function pemToDer(pem: string): Uint8Array {
  const base64 = pem
    .replace('-----BEGIN PUBLIC KEY-----', '')
    .replace('-----END PUBLIC KEY-----', '')
    .replace(/\s+/g, '');
  const binary = atob(base64);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

/**
 * Salt Edge signs callbacks as base64(RSA-SHA256(callback_url|post_body)).
 * The public key is supplied during production onboarding and kept as a
 * Cloudflare secret, rather than committed to the application.
 */
export async function verifySaltEdgeCallback(
  callbackUrl: string,
  postBody: string,
  signature: string,
  publicKeyPem: string,
): Promise<boolean> {
  try {
    const key = await crypto.subtle.importKey(
      'spki',
      pemToDer(publicKeyPem) as unknown as BufferSource,
      { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
      false,
      ['verify'],
    );
    const normalizedSignature = atob(signature);
    const signatureBytes = Uint8Array.from(normalizedSignature, (character) => character.charCodeAt(0));
    return crypto.subtle.verify(
      'RSASSA-PKCS1-v1_5',
      key,
      signatureBytes as unknown as BufferSource,
      new TextEncoder().encode(`${callbackUrl}|${postBody}`) as unknown as BufferSource,
    );
  } catch {
    return false;
  }
}
