/** Box prices typed as "7 3 11" or "7, 3, 11" (top to bottom), and the first token that is not a price. */
export type ParsedPrices = { readonly prices: number[]; readonly invalidToken?: string }

/** Parses one schuur's box prices; an empty text is an empty schuur. */
export function parsePrices(text: string): ParsedPrices {
  const tokens = text.split(/[\s,]+/).filter((token) => token !== '')
  return {
    prices: tokens.map(Number),
    invalidToken: tokens.find((token) => !/^[1-9]\d*$/.test(token)),
  }
}
