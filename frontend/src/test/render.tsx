import { QueryClient } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter } from 'react-router'
import { App, routes } from '../App'

/** Renders the whole app at `path`, with a fresh query cache and no retries. */
export function renderApp(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const user = userEvent.setup()
  return { user, router, ...render(<App queryClient={queryClient} router={router} />) }
}
