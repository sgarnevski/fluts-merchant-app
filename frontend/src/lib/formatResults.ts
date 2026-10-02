import type { EvaluationResult } from '../api/types'

/** The original output format of the specification, one block per scenario. */
export function formatResults(results: readonly EvaluationResult[]): string {
  return results
    .map(
      (result, index) =>
        `schuurs ${index + 1}\nMaximum profit is ${result.maxProfit}.\nNumber of fluts to buy: ${result.flutCounts.join(' ')}\n`,
    )
    .join('\n')
}
