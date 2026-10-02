import { useState } from 'react'
import { useEvaluate } from '../api/queries'
import type { ScenariosInput } from '../api/types'
import { ErrorMessage } from '../components/ErrorMessage'
import { FilePicker } from '../components/FilePicker'
import { ResultsTable } from '../components/ResultsTable'
import { formatResults } from '../lib/formatResults'

export const EXAMPLE_JSON = `{
  "scenarios": [
    { "name": "Example 1", "schuurs": [ { "boxPrices": [12, 3, 10, 7, 16, 5] } ] },
    { "name": "Example 2", "schuurs": [
        { "boxPrices": [7, 3, 11, 9, 10] },
        { "boxPrices": [1, 2, 3, 4, 10, 16, 10, 4, 16] } ] }
  ]
}`

/** Solve scenarios from pasted JSON or an uploaded .txt / .json file; they are stored as well. */
export function EvaluatorPage() {
  const [json, setJson] = useState(EXAMPLE_JSON)
  const [jsonError, setJsonError] = useState<string | null>(null)
  const evaluation = useEvaluate()

  const evaluateJson = () => {
    try {
      const input = JSON.parse(json) as ScenariosInput
      setJsonError(null)
      evaluation.mutate({ kind: 'json', input })
    } catch (error) {
      setJsonError(`Invalid JSON: ${(error as Error).message}`)
    }
  }

  return (
    <>
      <h1>Evaluate</h1>
      <div className="card">
        <label htmlFor="scenarios-json">Scenarios as JSON</label>
        <textarea id="scenarios-json" rows={10} value={json} onChange={(event) => setJson(event.target.value)} />
        <div className="row" style={{ marginTop: '0.75rem' }}>
          <button type="button" className="primary" disabled={evaluation.isPending} onClick={evaluateJson}>
            Evaluate JSON
          </button>
          <span className="muted">or</span>
          <FilePicker
            label="Evaluate a file…"
            disabled={evaluation.isPending}
            onFile={(file) => evaluation.mutate({ kind: 'file', file })}
          />
          <span className="muted">.txt in the original format, or .json</span>
        </div>
      </div>

      {jsonError && <ErrorMessage error={new Error(jsonError)} />}
      {evaluation.isError && <ErrorMessage error={evaluation.error} />}
      {evaluation.isSuccess && (
        <>
          <h2>Results</h2>
          <ResultsTable results={evaluation.data.results} />
          <h2>Original output format</h2>
          <pre>{formatResults(evaluation.data.results)}</pre>
        </>
      )}
    </>
  )
}
