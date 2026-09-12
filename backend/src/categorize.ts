import type { QuadrantIndex } from './types';

const urgentImportant = [
  'rent', 'mortgage', 'electric', 'energy', 'gas', 'water', 'tax',
  'insurance', 'loan', 'credit', 'medicine', 'pharmacy', 'medical',
  'czynsz', 'rata', 'prąd', 'gaz', 'woda', 'podatek', 'ubezpiec', 'leki',
];

const importantNotUrgent = [
  'saving', 'savings', 'investment', 'pension', 'education', 'course',
  'book', 'goal', 'emergency fund', 'oszczęd', 'inwest', 'emeryt',
  'eduk', 'kurs', 'książ', 'podusz', 'cel',
];

const lessImportantUrgent = [
  'fuel', 'transport', 'commute', 'repair', 'phone', 'internet', 'child',
  'grocery', 'groceries', 'shopping', 'paliw', 'transport', 'napraw',
  'telefon', 'internet', 'zakup', 'spożyw',
];

function includesAny(value: string, words: string[]): boolean {
  return words.some((word) => value.includes(word));
}

export function suggestQuadrant(description: string, category = ''): { index: QuadrantIndex; confidence: number } {
  const value = `${description} ${category}`.toLocaleLowerCase('pl-PL');
  if (includesAny(value, urgentImportant)) return { index: 0, confidence: 0.9 };
  if (includesAny(value, importantNotUrgent)) return { index: 1, confidence: 0.85 };
  if (includesAny(value, lessImportantUrgent)) return { index: 2, confidence: 0.7 };
  return { index: 3, confidence: 0.35 };
}
