import { suggestQuadrant } from './categorize';
import type { SaltEdgeTransaction } from './types';

export type NormalizedTransaction = {
  id: string;
  amountText: string;
  amountMinor: number;
  currencyCode: string;
  description: string;
  merchantName: string | null;
  providerCategory: string | null;
  madeOn: string;
  status: string;
  quadrantIndex: number | null;
  quadrantConfidence: number;
  providerUpdatedAt: string | null;
};

export function toMinorUnits(value: number | string): number {
  const parsed = typeof value === 'number' ? value : Number(value.replace(',', '.'));
  if (!Number.isFinite(parsed)) throw new Error(`Invalid bank amount: ${String(value)}`);
  return Math.round(parsed * 100);
}

export function normalizeTransaction(transaction: SaltEdgeTransaction): NormalizedTransaction {
  const description = transaction.description?.trim() || 'Transakcja bankowa';
  const category = transaction.category?.trim() || '';
  const merchantName = transaction.extra?.merchant_name?.trim() || null;
  const suggestion = suggestQuadrant(`${merchantName ?? ''} ${description}`, category);
  const amountMinor = toMinorUnits(transaction.amount);

  return {
    id: transaction.id,
    amountText: String(transaction.amount),
    amountMinor,
    currencyCode: transaction.currency_code,
    description,
    merchantName,
    providerCategory: category || null,
    madeOn: transaction.made_on,
    status: transaction.status || 'posted',
    quadrantIndex: amountMinor < 0 ? suggestion.index : null,
    quadrantConfidence: amountMinor < 0 ? suggestion.confidence : 0,
    providerUpdatedAt: transaction.updated_at || transaction.created_at || null,
  };
}
