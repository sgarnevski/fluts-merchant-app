import { LOGIN_URL } from '../api/client'

/**
 * Shown when a login through Keycloak could not be completed. Outside the normal layout on
 * purpose: the layout asks who is logged in, and a 401 there would send the user straight back
 * to the login instead of explaining what happened.
 */
export function SignInFailedPage() {
  return (
    <main style={{ maxWidth: '36rem', margin: '4rem auto', padding: '0 1.25rem' }}>
      <h1>Sign-in could not be completed</h1>
      <p className="muted">
        The login took too long or was interrupted. Your password was not the problem. Please try again.
      </p>
      <a className="button primary" href={LOGIN_URL}>
        Log in again
      </a>
    </main>
  )
}
