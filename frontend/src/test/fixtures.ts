import type { ScenarioPage, ScenarioResponse, ScenarioSummary } from '../api/types'

export const example2: ScenarioResponse = {
  id: 7,
  merchant: 'dutch',
  name: 'Example 2',
  inputType: 'JSON',
  schuurs: [
    { boxPrices: [7, 3, 11, 9, 10], maxProfit: 10, flutCounts: [2, 4, 5] },
    { boxPrices: [], maxProfit: 0, flutCounts: [0] },
    { boxPrices: [1, 2, 3, 4, 10, 16, 10, 4, 16], maxProfit: 30, flutCounts: [4, 5, 8] },
  ],
  maxProfit: 40,
  flutCounts: [6, 7, 8, 9, 10, 12, 13],
  createdAt: '2026-09-30T10:00:00Z',
  updatedAt: '2026-09-30T11:00:00Z',
}

export function summary(overrides: Partial<ScenarioSummary> = {}): ScenarioSummary {
  return {
    id: 7,
    merchant: 'dutch',
    name: 'Example 2',
    inputType: 'JSON',
    schuurCount: 2,
    maxProfit: 40,
    createdAt: '2026-09-30T10:00:00Z',
    updatedAt: '2026-09-30T11:00:00Z',
    ...overrides,
  }
}

export function page(content: ScenarioSummary[], number = 0, totalPages = 1): ScenarioPage {
  return { content, page: { number, size: 20, totalElements: content.length, totalPages } }
}
