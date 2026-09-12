import { describe, expect, it } from 'vitest';
import { suggestQuadrant } from '../src/categorize';

describe('financial quadrant suggestions', () => {
  it('recognizes important urgent obligations', () => {
    expect(suggestQuadrant('Czynsz za mieszkanie').index).toBe(0);
  });

  it('recognizes long-term goals', () => {
    expect(suggestQuadrant('Oszczędności na poduszkę finansową').index).toBe(1);
  });

  it('keeps unknown merchants as a low-confidence suggestion', () => {
    expect(suggestQuadrant('Sklep ABC').index).toBe(3);
    expect(suggestQuadrant('Sklep ABC').confidence).toBeLessThan(0.5);
  });
});
