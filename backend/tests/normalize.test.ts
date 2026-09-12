import { describe, expect, it } from 'vitest';
import { normalizeTransaction, toMinorUnits } from '../src/normalize';

describe('bank transaction normalization', () => {
  it('converts signed decimal amounts to minor units', () => {
    expect(toMinorUnits('-12.34')).toBe(-1234);
    expect(toMinorUnits('9,50')).toBe(950);
  });

  it('suggests a quadrant only for outgoing transactions', () => {
    const expense = normalizeTransaction({
      id: 'tx-1',
      account_id: 'account-1',
      amount: -1200,
      currency_code: 'PLN',
      description: 'Czynsz za mieszkanie',
      category: 'housing',
      made_on: '2026-09-10',
      status: 'posted',
    });
    const income = normalizeTransaction({
      id: 'tx-2',
      account_id: 'account-1',
      amount: 8000,
      currency_code: 'PLN',
      description: 'Pensja',
      made_on: '2026-09-01',
      status: 'posted',
    });
    expect(expense.amountMinor).toBe(-120000);
    expect(expense.quadrantIndex).toBe(0);
    expect(income.amountMinor).toBe(800000);
    expect(income.quadrantIndex).toBeNull();
  });
});
