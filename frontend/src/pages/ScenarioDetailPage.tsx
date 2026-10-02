import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useDeleteScenario, useScenario } from '../api/queries'
import { useCanEdit } from '../api/session'
import { ErrorMessage } from '../components/ErrorMessage'
import { MAX_SHOWN_BOXES, Pile } from '../components/Pile'
import styles from './ScenarioDetailPage.module.css'

/** Piles drawn per page. */
export const SCHUURS_PER_PAGE = 5

export function ScenarioDetailPage() {
  const id = Number(useParams().id)
  const scenario = useScenario(id)
  const deletion = useDeleteScenario()
  const navigate = useNavigate()
  const canEdit = useCanEdit()
  const [page, setPage] = useState(0)

  if (scenario.isPending) {
    return <p className="muted">Loading…</p>
  }
  if (scenario.isError) {
    return <ErrorMessage error={scenario.error} />
  }

  const { data } = scenario
  const fileUrl = `/api/scenarios/${id}/file`
  const pages = Math.ceil(data.schuurs.length / SCHUURS_PER_PAGE)
  const first = page * SCHUURS_PER_PAGE
  const shown = data.schuurs.slice(first, first + SCHUURS_PER_PAGE)
  const confirmDelete = () => {
    if (window.confirm(`Delete "${data.name}"?`)) {
      deletion.mutate(id, { onSuccess: () => navigate('/') })
    }
  }

  return (
    <>
      <div className="row">
        <h1>{data.name}</h1>
        <span style={{ flex: 1 }} />
        <a className="button" href={fileUrl} download>
          Download scenario
        </a>
        <a className="button" href={`/api/scenarios/${id}/result/file`} download>
          Download result
        </a>
        {canEdit && (
          <>
            <Link to={`/scenarios/${id}/edit`} className="button">
              Edit
            </Link>
            <button type="button" className="danger" onClick={confirmDelete}>
              Delete
            </button>
          </>
        )}
      </div>
      {deletion.isError && <ErrorMessage error={deletion.error} />}

      <dl className={`card ${styles.stats}`}>
        <div className={styles.stat}>
          <dt>Maximum profit</dt>
          <dd>{data.maxProfit} florins</dd>
        </div>
        <div className={styles.stat}>
          <dt>Number of fluts to buy</dt>
          <dd>{data.flutCounts.join(' ')}</dd>
        </div>
        <div className={styles.stat}>
          <dt>Input</dt>
          <dd>{data.inputType}</dd>
        </div>
        <div className={styles.stat}>
          <dt>Last changed</dt>
          <dd style={{ fontSize: '1rem' }}>{new Date(data.updatedAt).toLocaleString()}</dd>
        </div>
      </dl>

      <h2>Schuurs</h2>
      <p className={`muted ${styles.legend}`}>
        Top box first. Outlined boxes are the ones to buy, counted from the top. Green boxes cost less than the
        10 florins a flut sells for, red ones more. Piles show at most {MAX_SHOWN_BOXES} boxes; the scenario file has
        them all, in the format of the specification, and so does the result file.
      </p>
      {pages > 1 && (
        <div className="row" style={{ marginBottom: '0.75rem' }}>
          <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </button>
          <span className="muted">
            Schuurs {first + 1}–{first + shown.length} of {data.schuurs.length}
          </span>
          <button type="button" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>
            Next
          </button>
        </div>
      )}
      <div className={`card ${styles.piles}`}>
        {shown.map((schuur, index) => (
          <Pile
            key={first + index}
            title={`Schuur ${first + index + 1}`}
            schuur={schuur}
            fileUrl={`/api/scenarios/${id}/schuurs/${first + index + 1}/file`}
            resultUrl={`/api/scenarios/${id}/schuurs/${first + index + 1}/result/file`}
          />
        ))}
      </div>
    </>
  )
}
