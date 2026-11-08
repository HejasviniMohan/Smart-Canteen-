import { describe, expect, it } from 'vitest'
import { money, slotLabel } from './format'

describe('money', () => {
  it('hides paise on whole rupees', () => expect(money('65.00')).toBe('₹65'))
  it('shows paise when present', () => expect(money(65.5)).toBe('₹65.50'))
})

describe('slotLabel', () => {
  const now = new Date(2026, 10, 7, 10, 0)
  it('says Today for a slot later today', () => expect(slotLabel(new Date(2026, 10, 7, 12, 0), now)).toMatch(/^Today/))
  it('says Tomorrow for a slot the next day', () => expect(slotLabel(new Date(2026, 10, 8, 9, 0), now)).toMatch(/^Tomorrow/))
})
