import { Link, NavLink, Outlet } from 'react-router'
import { logout, useCanEdit, useMe } from '../api/session'
import styles from './Layout.module.css'

export function Layout() {
  const me = useMe()
  const canEdit = useCanEdit()
  return (
    <>
      <header className={styles.header}>
        <div className={styles.bar}>
          <Link to="/" className={styles.brand}>
            Trading of Fluts
          </Link>
          <nav className={styles.nav}>
            <NavLink to="/" end>
              Scenarios
            </NavLink>
            {canEdit && <NavLink to="/scenarios/new">New scenario</NavLink>}
            {canEdit && <NavLink to="/evaluate">Evaluate</NavLink>}
          </nav>
          {me.isSuccess && (
            <div className={styles.user}>
              <span className={styles.merchant}>{me.data.merchant?.name ?? 'All merchants'}</span>
              <span className="muted">{me.data.username}</span>
              <button type="button" onClick={() => void logout()}>
                Log out
              </button>
            </div>
          )}
        </div>
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
    </>
  )
}
