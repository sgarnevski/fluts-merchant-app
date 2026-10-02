import { useState, type FormEvent } from 'react'
import type { ScenarioRequest } from '../api/types'
import { ErrorMessage } from '../components/ErrorMessage'
import { parsePrices } from '../lib/parsePrices'

export type ScenarioFormValues = { readonly name: string; readonly schuurs: readonly string[] }

/** Name plus one text field per schuur with its box prices, top to bottom. */
export function ScenarioForm({
  initial,
  submitLabel,
  pending,
  error,
  onSubmit,
}: {
  readonly initial: ScenarioFormValues
  readonly submitLabel: string
  readonly pending: boolean
  readonly error: unknown
  readonly onSubmit: (request: ScenarioRequest) => void
}) {
  const [name, setName] = useState(initial.name)
  const [schuurs, setSchuurs] = useState<readonly string[]>(initial.schuurs)
  const [inputError, setInputError] = useState<string | null>(null)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    const parsed = schuurs.map(parsePrices)
    const invalidIndex = parsed.findIndex((result) => result.invalidToken !== undefined)
    if (invalidIndex >= 0) {
      setInputError(`Schuur ${invalidIndex + 1}: "${parsed[invalidIndex].invalidToken}" is not a positive whole number`)
      return
    }
    setInputError(null)
    onSubmit({ name: name.trim(), schuurs: parsed.map((result) => ({ boxPrices: result.prices })) })
  }

  return (
    <form className="card" onSubmit={submit}>
      <label htmlFor="scenario-name">Name</label>
      <input id="scenario-name" value={name} onChange={(event) => setName(event.target.value)} />

      <h2>Schuurs</h2>
      <p className="muted">Box prices from top to bottom, separated by spaces or commas. Leave empty for an empty pile.</p>
      {schuurs.map((prices, index) => (
        <div className="row" key={index} style={{ marginBottom: '0.5rem' }}>
          <input
            aria-label={`Schuur ${index + 1} box prices`}
            placeholder="e.g. 7 3 11 9 10"
            value={prices}
            style={{ flex: 1 }}
            onChange={(event) => setSchuurs(schuurs.map((text, at) => (at === index ? event.target.value : text)))}
          />
          <button
            type="button"
            aria-label={`Remove schuur ${index + 1}`}
            disabled={schuurs.length === 1}
            onClick={() => setSchuurs(schuurs.filter((_, at) => at !== index))}
          >
            Remove
          </button>
        </div>
      ))}
      <button type="button" onClick={() => setSchuurs([...schuurs, ''])}>
        Add schuur
      </button>

      {inputError && (
        <p role="alert" className="muted" style={{ color: 'var(--color-loss)' }}>
          {inputError}
        </p>
      )}
      {error !== null && <ErrorMessage error={error} />}

      <div className="row" style={{ marginTop: '1.25rem' }}>
        <button type="submit" className="primary" disabled={pending}>
          {submitLabel}
        </button>
      </div>
    </form>
  )
}
