import { normalizeTransaction } from './normalize';
import {
  finishSyncRun,
  getSaltEdgeCustomer,
  markConnectionSynced,
  saveAccount,
  saveTransaction,
  startSyncRun,
  upsertConnection,
} from './db';
import { SaltEdgeClient } from './saltEdge';
import type { Env } from './types';

function syncId(userId: string): string {
  return `${userId}-${crypto.randomUUID()}`;
}

export async function syncUser(userId: string, env: Env): Promise<{ accounts: number; transactions: number }> {
  const runId = syncId(userId);
  await startSyncRun(env.DB, userId, runId);
  let accountsSeen = 0;
  let transactionsSeen = 0;
  try {
    const customer = await getSaltEdgeCustomer(env.DB, userId);
    if (!customer) throw new Error('No Salt Edge customer exists for this user');
    const client = new SaltEdgeClient(env);
    const connections = await client.listConnections(customer.customer_id);

    for (const connection of connections) {
      await upsertConnection(env.DB, userId, connection);
      const accounts = await client.listAccounts(connection.id);
      accountsSeen += accounts.length;
      for (const account of accounts) {
        await saveAccount(env.DB, userId, connection.id, account);
        const transactions = await client.listTransactions(connection.id, account.id);
        for (const transaction of transactions) {
          const normalized = normalizeTransaction(transaction);
          // Salt Edge IDs are globally unique in normal operation. Prefixing
          // with the account keeps local test providers collision-safe.
          normalized.id = `${account.id}:${normalized.id}`;
          await saveTransaction(env.DB, userId, connection.id, account.id, normalized);
        }
        transactionsSeen += transactions.length;
      }
      await markConnectionSynced(env.DB, connection.id);
    }

    await finishSyncRun(env.DB, runId, 'success', accountsSeen, transactionsSeen);
    return { accounts: accountsSeen, transactions: transactionsSeen };
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown sync error';
    await finishSyncRun(env.DB, runId, 'failure', accountsSeen, transactionsSeen, message);
    throw error;
  }
}

export async function syncAllDueUsers(env: Env): Promise<void> {
  const result = await env.DB.prepare(
    `SELECT DISTINCT user_id FROM bank_connections
     WHERE status = 'connected'
       AND (last_sync_at IS NULL OR last_sync_at < datetime('now', '-1 day'))`,
  ).all<{ user_id: string }>();
  await Promise.allSettled(result.results.map(({ user_id }) => syncUser(user_id, env)));
}
