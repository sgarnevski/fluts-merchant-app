import { describe, expect, it } from 'vitest'
import { formatResults } from './formatResults'

describe('formatResults', () => {
  it('produces the original output format with a blank line between scenarios', () => {
    expect(
      formatResults([
        { scenario: 'Example 1', maxProfit: 8, flutCounts: [4] },
        { scenario: 'Example 2', maxProfit: 40, flutCounts: [6, 7, 8, 9, 10, 12, 13] },
      ]),
    ).toBe(
      'schuurs 1\nMaximum profit is 8.\nNumber of fluts to buy: 4\n\n' +
        'schuurs 2\nMaximum profit is 40.\nNumber of fluts to buy: 6 7 8 9 10 12 13\n',
    )
  })
})
