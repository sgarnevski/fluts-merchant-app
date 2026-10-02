import { ApiError } from '../api/client'
import styles from './ErrorMessage.module.css'

/** Shows what went wrong; for API errors also the line number and every invalid field. */
export function ErrorMessage({ error }: { readonly error: unknown }) {
  const problem = error instanceof ApiError ? error.problem : undefined
  const message = error instanceof Error ? error.message : 'Something went wrong'
  return (
    <div role="alert" className={styles.error}>
      {problem?.line === undefined ? message : `Line ${problem.line}: ${message}`}
      {problem?.errors && (
        <ul>
          {problem.errors.map((detail) => (
            <li key={detail}>{detail}</li>
          ))}
        </ul>
      )}
    </div>
  )
}
