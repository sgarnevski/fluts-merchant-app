import { Link } from 'react-router'
import type { EvaluationResult } from '../api/types'

export function ResultsTable({ results }: { readonly results: readonly EvaluationResult[] }) {
  return (
    <table>
      <thead>
        <tr>
          <th>Scenario</th>
          <th>Maximum profit</th>
          <th>Number of fluts to buy</th>
        </tr>
      </thead>
      <tbody>
        {results.map((result, index) => (
          <tr key={index}>
            <td>{result.id === undefined ? result.scenario : <Link to={`/scenarios/${result.id}`}>{result.scenario}</Link>}</td>
            <td>{result.maxProfit}</td>
            <td>{result.flutCounts.join(' ')}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
