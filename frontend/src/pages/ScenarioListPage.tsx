import { useState } from 'react'
import { Link } from 'react-router'
import { useDeleteScenario, useImportFile, useScenarioPage } from '../api/queries'
import { useCanEdit, useIsAdmin, useMerchants } from '../api/session'
import type { InputType, ScenarioSummary } from '../api/types'
import { ErrorMessage } from '../components/ErrorMessage'
import { FilePicker } from '../components/FilePicker'

const PAGE_SIZE = 20

const SORTS = [
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'createdAt,asc', label: 'Oldest first' },
  { value: 'name,asc', label: 'Name A–Z' },
  { value: 'updatedAt,desc', label: 'Recently changed' },
] as const

export function ScenarioListPage() {
  const [page, setPage] = useState(0)
  const [inputType, setInputType] = useState<InputType | ''>('')
  const [name, setName] = useState('')
  const [sort, setSort] = useState<string>(SORTS[0].value)
  const [merchant, setMerchant] = useState('')
  const isAdmin = useIsAdmin()
  const merchants = useMerchants()
  const scenarios = useScenarioPage({
    page,
    size: PAGE_SIZE,
    sort,
    merchant: merchant || undefined,
    inputType: inputType || undefined,
    name: name || undefined,
  })
  const merchantName = (code: string) => merchants.data?.find((candidate) => candidate.code === code)?.name ?? code
  const deletion = useDeleteScenario()
  const importing = useImportFile()
  const canEdit = useCanEdit()

  const confirmDelete = (scenario: ScenarioSummary) => {
    if (window.confirm(`Delete "${scenario.name}"?`)) {
      deletion.mutate(scenario.id)
    }
  }

  return (
    <>
      <div className="row">
        <h1>Scenarios</h1>
        <span style={{ flex: 1 }} />
        {canEdit && (
          <>
            <FilePicker label="Import file…" disabled={importing.isPending} onFile={(file) => importing.mutate(file)} />
            <Link to="/scenarios/new" className="button primary">
              New scenario
            </Link>
          </>
        )}
      </div>

      <div className="row" style={{ marginBottom: '1rem' }}>
        <input
          aria-label="Search by name"
          placeholder="Search by name"
          value={name}
          onChange={(event) => {
            setName(event.target.value)
            setPage(0)
          }}
        />
        <select
          aria-label="Input type"
          value={inputType}
          onChange={(event) => {
            setInputType(event.target.value as InputType | '')
            setPage(0)
          }}
        >
          <option value="">All input types</option>
          <option value="JSON">JSON</option>
          <option value="FILE">File</option>
          <option value="KAFKA">Kafka</option>
        </select>
        {isAdmin && (
          <select
            aria-label="Merchant"
            value={merchant}
            onChange={(event) => {
              setMerchant(event.target.value)
              setPage(0)
            }}
          >
            <option value="">All merchants</option>
            {(merchants.data ?? []).map((candidate) => (
              <option key={candidate.code} value={candidate.code}>
                {candidate.name}
              </option>
            ))}
          </select>
        )}
        <select aria-label="Sort" value={sort} onChange={(event) => setSort(event.target.value)}>
          {SORTS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {importing.isSuccess && <p className="muted">Imported {importing.data.results.length} scenario(s).</p>}
      {importing.isError && <ErrorMessage error={importing.error} />}
      {deletion.isError && <ErrorMessage error={deletion.error} />}
      {scenarios.isError && <ErrorMessage error={scenarios.error} />}
      {scenarios.isPending && <p className="muted">Loading…</p>}

      {scenarios.isSuccess &&
        (scenarios.data.content.length === 0 ? (
          <p className="muted">No scenarios found.</p>
        ) : (
          <>
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  {isAdmin && <th>Merchant</th>}
                  <th>Input</th>
                  <th>Schuurs</th>
                  <th>Maximum profit</th>
                  {canEdit && <th />}
                </tr>
              </thead>
              <tbody>
                {scenarios.data.content.map((scenario) => (
                  <tr key={scenario.id}>
                    <td>
                      <Link to={`/scenarios/${scenario.id}`}>{scenario.name}</Link>
                    </td>
                    {isAdmin && <td>{merchantName(scenario.merchant)}</td>}
                    <td>{scenario.inputType}</td>
                    <td>{scenario.schuurCount}</td>
                    <td>{scenario.maxProfit}</td>
                    {canEdit && (
                      <td className="row" style={{ justifyContent: 'flex-end' }}>
                        <Link to={`/scenarios/${scenario.id}/edit`}>Edit</Link>
                        <button type="button" className="danger" onClick={() => confirmDelete(scenario)}>
                          Delete
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="row" style={{ marginTop: '1rem' }}>
              <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>
                Previous
              </button>
              <span className="muted">
                Page {page + 1} of {scenarios.data.page.totalPages}
              </span>
              <button
                type="button"
                disabled={page + 1 >= scenarios.data.page.totalPages}
                onClick={() => setPage(page + 1)}
              >
                Next
              </button>
            </div>
          </>
        ))}
    </>
  )
}
