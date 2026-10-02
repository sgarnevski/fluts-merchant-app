import { describe, expect, it } from 'vitest'
import { parsePrices } from './parsePrices'

describe('parsePrices', () => {
  it('reads prices separated by spaces or commas', () => {
    expect(parsePrices(' 7, 3  11,9 ')).toEqual({ prices: [7, 3, 11, 9], invalidToken: undefined })
  })

  it('reads an empty text as an empty schuur', () => {
    expect(parsePrices('  ')).toEqual({ prices: [], invalidToken: undefined })
  })

  it.each(['0', '-3', '2.5', 'x', '07'])('reports %s as invalid', (token) => {
    expect(parsePrices(`5 ${token} 6`).invalidToken).toBe(token)
  })
})
