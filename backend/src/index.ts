import { cors } from 'hono/cors';
import { Hono } from 'hono';
import { authenticate } from './auth';
import {
  ensureUser,
  getConnectionForUser,
  getSaltEdgeCustomer,
  getUserIdBySaltEdgeCustomer,
  confirmTransactionQuadrant,
  listTransactions,
  listUserConnections,
  markConsentRevoked,
  savePendingConsent,
  saveSaltEdgeCustomer,
} from './db';
import { SaltEdgeClient } from './saltEdge';
import { verifySaltEdgeCallback } from './signature';
import { syncAllDueUsers, syncUser } from './sync';
import type { AuthUser, Env } from './types';

interface Variables {
  user: AuthUser;
}

const app = new Hono<{ Bindings: Env; Variables: Variables }>();

app.use('/api/*', cors({ origin: '*' }));

app.get('/api/health', (c) => c.json({ ok: true, service: 'fokus-bank-api' }));

app.use('/api/bank/*', async (c, next) => {
  // Salt Edge calls this route directly and authenticates it with its RSA
  // callback signature, not with an end-user JWT.
  if (c.req.path === '/api/bank/callback') return next();
  try {
    const user = await authenticate(c.req.raw, c.env);
    await ensureUser(c.env.DB, user);
    c.set('user', user);
    await next();
  } catch (error) {
    return c.json({ error: 'unauthorized', message: error instanceof Error ? error.message : 'Authentication required' }, 401);
  }
});

app.get('/api/bank/providers', async (c) => {
  const country = c.req.query('country') || undefined;
  const providers = await new SaltEdgeClient(c.env).listProviders(country);
  return c.json({ data: providers });
});

app.post('/api/bank/connect', async (c) => {
  const user = c.get('user');
  let body: { historyDays?: number } = {};
  try {
    body = await c.req.json<{ historyDays?: number }>();
  } catch {
    // An empty body uses the default provider history window.
  }
  const historyDays = Math.min(Math.max(Number(body.historyDays || 3650), 1), 3650);
  const client = new SaltEdgeClient(c.env);
  let customer = await getSaltEdgeCustomer(c.env.DB, user.id);
  if (!customer) {
    const created = await client.createLead(user.email);
    await saveSaltEdgeCustomer(c.env.DB, user.id, created.customerId);
    customer = { customer_id: created.customerId };
  }
  const session = await client.createLeadSession(customer.customer_id, historyDays);
  await savePendingConsent(c.env.DB, user.id, customer.customer_id, crypto.randomUUID(), historyDays);
  return c.json({
    provider: 'salt_edge',
    authorizationUrl: session.redirectUrl,
    expiresAt: session.expiresAt || null,
    historyDays,
  });
});

app.get('/api/bank/connections', async (c) => {
  const user = c.get('user');
  return c.json({ data: await listUserConnections(c.env.DB, user.id) });
});

app.post('/api/bank/sync', async (c) => {
  const user = c.get('user');
  const result = await syncUser(user.id, c.env);
  return c.json({ ok: true, ...result });
});

app.get('/api/bank/transactions', async (c) => {
  const user = c.get('user');
  const limit = Number(c.req.query('limit') || 250);
  return c.json({ data: await listTransactions(c.env.DB, user.id, limit) });
});

app.patch('/api/bank/transactions/:id/quadrant', async (c) => {
  const user = c.get('user');
  const body = await c.req.json<{ quadrantIndex?: number }>().catch(() => ({} as { quadrantIndex?: number }));
  const quadrantIndex = body.quadrantIndex;
  if (quadrantIndex === undefined || !Number.isInteger(quadrantIndex) || quadrantIndex < 0 || quadrantIndex > 3) {
    return c.json({ error: 'quadrant_index_invalid' }, 400);
  }
  const updated = await confirmTransactionQuadrant(c.env.DB, user.id, c.req.param('id'), quadrantIndex);
  return updated ? c.json({ ok: true }) : c.json({ error: 'transaction_not_found' }, 404);
});

app.delete('/api/bank/connections/:id', async (c) => {
  const user = c.get('user');
  const id = c.req.param('id');
  const connection = await getConnectionForUser(c.env.DB, user.id, id);
  if (!connection) return c.json({ error: 'connection_not_found' }, 404);
  const client = new SaltEdgeClient(c.env);
  const consents = await client.listPartnerConsents(id);
  const activeConsent = consents.find((consent) => consent.status === 'active')
    || (connection.partner_consent_id ? consents.find((consent) => consent.id === connection.partner_consent_id) : undefined);
  if (!activeConsent) return c.json({ error: 'active_provider_consent_not_found' }, 409);
  await client.revokePartnerConsent(activeConsent.id, id);
  await markConsentRevoked(c.env.DB, user.id, id);
  return c.json({ ok: true });
});

// Salt Edge sends the exact callback body and a Signature header. Verify before
// using the customer id to start a synchronization job.
app.post('/api/bank/callback', async (c) => {
  const publicKey = c.env.SALTEDGE_CALLBACK_PUBLIC_KEY;
  if (!publicKey) return c.json({ error: 'callback_verification_not_configured' }, 503);
  const body = await c.req.text();
  const signature = c.req.header('Signature');
  if (!signature) return c.json({ error: 'callback_signature_missing' }, 401);
  const verified = await verifySaltEdgeCallback(c.req.raw.url, body, signature, publicKey);
  if (!verified) return c.json({ error: 'callback_signature_invalid' }, 401);

  let payload: { data?: { customer_id?: string; connection_id?: string } };
  try {
    payload = JSON.parse(body) as { data?: { customer_id?: string; connection_id?: string } };
  } catch {
    return c.json({ error: 'callback_body_invalid' }, 400);
  }
  const customerId = payload.data?.customer_id;
  if (customerId) {
    const userId = await getUserIdBySaltEdgeCustomer(c.env.DB, customerId);
    if (userId) c.executionCtx.waitUntil(syncUser(userId, c.env).catch((error) => console.error(error)));
  }
  return c.json({ accepted: true }, 202);
});

app.onError((error, c) => {
  console.error(error);
  return c.json({
    error: 'internal_error',
    message: c.env.ENVIRONMENT === 'development' && error instanceof Error ? error.message : 'Unexpected server error',
  }, 500);
});

export default {
  fetch: app.fetch,
  scheduled: (_controller: ScheduledController, env: Env, ctx: ExecutionContext) => {
    ctx.waitUntil(syncAllDueUsers(env));
  },
};

export { app };
