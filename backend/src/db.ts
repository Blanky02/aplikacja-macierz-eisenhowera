import type { AuthUser, SaltEdgeAccount, SaltEdgeConnection } from './types';
import type { NormalizedTransaction } from './normalize';

export async function ensureUser(db: D1Database, user: AuthUser): Promise<void> {
  const now = new Date().toISOString();
  await db.prepare(
    `INSERT INTO users (id, email, created_at, updated_at)
     VALUES (?, ?, ?, ?)
     ON CONFLICT(id) DO UPDATE SET email = excluded.email, updated_at = excluded.updated_at`,
  ).bind(user.id, user.email, now, now).run();
}

export async function getSaltEdgeCustomer(db: D1Database, userId: string): Promise<{ customer_id: string } | null> {
  return db.prepare('SELECT customer_id FROM saltedge_customers WHERE user_id = ?').bind(userId).first<{ customer_id: string }>();
}

export async function saveSaltEdgeCustomer(db: D1Database, userId: string, customerId: string): Promise<void> {
  await db.prepare(
    `INSERT INTO saltedge_customers (user_id, customer_id)
     VALUES (?, ?)
     ON CONFLICT(user_id) DO UPDATE SET customer_id = excluded.customer_id`,
  ).bind(userId, customerId).run();
}

export async function getUserByEmail(db: D1Database, email: string): Promise<AuthUser | null> {
  return db.prepare('SELECT id, email FROM users WHERE email = ?').bind(email).first<AuthUser>();
}

export async function getUserById(db: D1Database, userId: string): Promise<AuthUser | null> {
  return db.prepare('SELECT id, email FROM users WHERE id = ?').bind(userId).first<AuthUser>();
}

export async function upsertConnection(db: D1Database, userId: string, connection: SaltEdgeConnection): Promise<void> {
  const now = new Date().toISOString();
  await db.prepare(
    `INSERT INTO bank_connections
      (id, user_id, provider_code, provider_name, partner_consent_id, status, last_sync_at, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT(id) DO UPDATE SET
       provider_code = excluded.provider_code,
       provider_name = excluded.provider_name,
       partner_consent_id = COALESCE(excluded.partner_consent_id, bank_connections.partner_consent_id),
       status = excluded.status,
       updated_at = excluded.updated_at`,
  ).bind(
    connection.id,
    userId,
    connection.provider_code,
    connection.provider_name,
    connection.partner_consent_id || null,
    connection.status === 'disconnected' ? 'error' : 'connected',
    connection.last_success_at || null,
    connection.created_at || now,
    now,
  ).run();

  await db.prepare(
    `UPDATE bank_consents
     SET connection_id = ?, provider_consent_id = COALESCE(?, provider_consent_id), status = 'active', updated_at = ?
     WHERE user_id = ? AND customer_id = ? AND status = 'pending'`,
  ).bind(connection.id, connection.partner_consent_id || null, now, userId, connection.customer_id).run();
}

export async function listUserConnections(db: D1Database, userId: string): Promise<unknown[]> {
  const result = await db.prepare(
    `SELECT id, provider_code, provider_name, partner_consent_id, status, last_sync_at, created_at, updated_at
     FROM bank_connections WHERE user_id = ? ORDER BY created_at DESC`,
  ).bind(userId).all();
  return result.results;
}

export async function savePendingConsent(
  db: D1Database,
  userId: string,
  customerId: string,
  consentId: string,
  historyDays: number,
): Promise<void> {
  const fromDate = new Date();
  fromDate.setUTCDate(fromDate.getUTCDate() - historyDays);
  await db.prepare(
    `INSERT INTO bank_consents
      (id, user_id, customer_id, scopes_json, from_date, period_days, status)
     VALUES (?, ?, ?, ?, ?, ?, 'pending')`,
  ).bind(
    consentId,
    userId,
    customerId,
    JSON.stringify(['account_details', 'transactions_details']),
    fromDate.toISOString().slice(0, 10),
    historyDays,
  ).run();
}

export async function getConnectionForUser(
  db: D1Database,
  userId: string,
  connectionId: string,
): Promise<{ id: string; partner_consent_id: string | null } | null> {
  return db.prepare(
    `SELECT id, partner_consent_id FROM bank_connections WHERE id = ? AND user_id = ?`,
  ).bind(connectionId, userId).first<{ id: string; partner_consent_id: string | null }>();
}

export async function markConsentRevoked(db: D1Database, userId: string, connectionId: string): Promise<void> {
  const now = new Date().toISOString();
  await db.batch([
    db.prepare(
      `UPDATE bank_connections SET status = 'disconnected', updated_at = ? WHERE id = ? AND user_id = ?`,
    ).bind(now, connectionId, userId),
    db.prepare(
      `UPDATE bank_consents SET status = 'revoked', revoked_at = ?, updated_at = ? WHERE connection_id = ? AND user_id = ?`,
    ).bind(now, now, connectionId, userId),
  ]);
}

export async function getUserIdBySaltEdgeCustomer(db: D1Database, customerId: string): Promise<string | null> {
  const row = await db.prepare('SELECT user_id FROM saltedge_customers WHERE customer_id = ?').bind(customerId).first<{ user_id: string }>();
  return row?.user_id || null;
}

export async function saveAccount(db: D1Database, userId: string, connectionId: string, account: SaltEdgeAccount): Promise<void> {
  const balanceMinor = typeof account.balance === 'number' ? Math.round(account.balance * 100) : null;
  await db.prepare(
    `INSERT INTO bank_accounts
      (id, user_id, connection_id, name, nature, balance_minor, currency_code, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT(id) DO UPDATE SET
       name = excluded.name,
       nature = excluded.nature,
       balance_minor = excluded.balance_minor,
       currency_code = excluded.currency_code,
       updated_at = excluded.updated_at`,
  ).bind(
    account.id,
    userId,
    connectionId,
    account.name,
    account.nature || null,
    balanceMinor,
    account.currency_code,
    account.updated_at || new Date().toISOString(),
  ).run();
}

export async function saveTransaction(
  db: D1Database,
  userId: string,
  connectionId: string,
  accountId: string,
  transaction: NormalizedTransaction,
): Promise<void> {
  await db.prepare(
    `INSERT INTO bank_transactions
      (id, user_id, connection_id, account_id, amount_text, amount_minor, currency_code,
       description, merchant_name, provider_category, made_on, status, quadrant_index,
       quadrant_confidence, provider_updated_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT(id) DO UPDATE SET
       amount_text = excluded.amount_text,
       amount_minor = excluded.amount_minor,
       currency_code = excluded.currency_code,
       description = excluded.description,
       merchant_name = excluded.merchant_name,
       provider_category = excluded.provider_category,
       made_on = excluded.made_on,
       status = excluded.status,
       quadrant_index = CASE WHEN bank_transactions.quadrant_confirmed = 1 THEN bank_transactions.quadrant_index ELSE excluded.quadrant_index END,
       quadrant_confidence = CASE WHEN bank_transactions.quadrant_confirmed = 1 THEN bank_transactions.quadrant_confidence ELSE excluded.quadrant_confidence END,
       provider_updated_at = excluded.provider_updated_at,
       updated_at = excluded.updated_at`,
  ).bind(
    transaction.id,
    userId,
    connectionId,
    accountId,
    transaction.amountText,
    transaction.amountMinor,
    transaction.currencyCode,
    transaction.description,
    transaction.merchantName,
    transaction.providerCategory,
    transaction.madeOn,
    transaction.status,
    transaction.quadrantIndex,
    transaction.quadrantConfidence,
    transaction.providerUpdatedAt,
    new Date().toISOString(),
  ).run();
}

export async function markConnectionSynced(db: D1Database, connectionId: string): Promise<void> {
  const now = new Date().toISOString();
  await db.prepare(
    `UPDATE bank_connections SET last_sync_at = ?, updated_at = ? WHERE id = ?`,
  ).bind(now, now, connectionId).run();
}

export async function listTransactions(db: D1Database, userId: string, limit = 250): Promise<unknown[]> {
  const safeLimit = Math.min(Math.max(limit, 1), 500);
  const result = await db.prepare(
    `SELECT id, account_id, amount_text, amount_minor, currency_code, description,
            merchant_name, provider_category, made_on, status, quadrant_index,
            quadrant_confidence, quadrant_confirmed, imported_at, updated_at
     FROM bank_transactions WHERE user_id = ? ORDER BY made_on DESC, imported_at DESC LIMIT ?`,
  ).bind(userId, safeLimit).all();
  return result.results;
}

export async function confirmTransactionQuadrant(
  db: D1Database,
  userId: string,
  transactionId: string,
  quadrantIndex: number,
): Promise<boolean> {
  const result = await db.prepare(
    `UPDATE bank_transactions
     SET quadrant_index = ?, quadrant_confirmed = 1, updated_at = ?
     WHERE id = ? AND user_id = ?`,
  ).bind(quadrantIndex, new Date().toISOString(), transactionId, userId).run();
  return result.meta.changes === 1;
}

export async function startSyncRun(db: D1Database, userId: string, id: string): Promise<void> {
  await db.prepare(
    `INSERT INTO sync_runs (id, user_id, started_at, status) VALUES (?, ?, ?, 'running')`,
  ).bind(id, userId, new Date().toISOString()).run();
}

export async function finishSyncRun(
  db: D1Database,
  id: string,
  status: 'success' | 'failure',
  accounts: number,
  transactions: number,
  errorMessage?: string,
): Promise<void> {
  await db.prepare(
    `UPDATE sync_runs SET finished_at = ?, status = ?, accounts_seen = ?, transactions_seen = ?, error_message = ? WHERE id = ?`,
  ).bind(new Date().toISOString(), status, accounts, transactions, errorMessage || null, id).run();
}
