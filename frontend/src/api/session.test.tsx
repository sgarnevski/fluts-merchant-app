import { screen, waitFor } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { browser } from '../lib/browser'
import { example2, page, summary } from '../test/fixtures'
import { renderApp } from '../test/render'
import { ADMIN, server, VIEWER } from '../test/server'
import { LOGIN_URL } from './client'

afterEach(() => {
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
})

describe('session', () => {
  it('shows the logged-in user and their merchant', async () => {
    server.use(http.get('*/api/scenarios', () => HttpResponse.json(page([]))))
    renderApp('/')

    expect(await screen.findByText('dutch')).toBeInTheDocument()
    expect(screen.getByText('Dutch merchant')).toBeInTheDocument()
    expect(screen.queryByLabelText('Merchant')).toBeNull()
  })

  it('lets admins see and filter every merchant, read-only', async () => {
    const queries: URLSearchParams[] = []
    server.use(
      http.get('*/api/me', () => HttpResponse.json(ADMIN)),
      http.get('*/api/merchants', async () => {
        await delay(300)
        return HttpResponse.json([{ code: 'dutch', name: 'Dutch merchant' }, { code: 'french', name: 'French merchant' }])
      }),
      http.get('*/api/scenarios', ({ request }) => {
        queries.push(new URL(request.url).searchParams)
        return HttpResponse.json(page([summary(), summary({ id: 8, name: 'Foreign', merchant: 'spanish' })]))
      }),
    )
    const { user } = renderApp('/')

    expect(await screen.findByText('All merchants', { selector: 'span' })).toBeInTheDocument()
    expect(await screen.findByRole('columnheader', { name: 'Merchant' })).toBeInTheDocument()
    expect(screen.getAllByRole('cell', { name: 'dutch' })).toHaveLength(1)
    expect(await screen.findByRole('cell', { name: 'Dutch merchant' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'spanish' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete' })).toBeNull()

    await user.selectOptions(screen.getByLabelText('Merchant'), 'french')

    await waitFor(() => expect(queries.at(-1)?.get('merchant')).toBe('french'))
  })

  it('hides every change for viewers', async () => {
    server.use(
      http.get('*/api/me', () => HttpResponse.json(VIEWER)),
      http.get('*/api/scenarios', () => HttpResponse.json(page([summary()]))),
      http.get('*/api/scenarios/:id', () => HttpResponse.json(example2)),
    )
    const { user } = renderApp('/')

    await screen.findByText('viewer')
    expect(screen.getByRole('link', { name: 'Example 2' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'New scenario' })).toBeNull()
    expect(screen.queryByRole('link', { name: 'Evaluate' })).toBeNull()
    expect(screen.queryByLabelText('Import file…')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Delete' })).toBeNull()

    await user.click(screen.getByRole('link', { name: 'Example 2' }))
    expect(await screen.findByText('40 florins')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Edit' })).toBeNull()
  })

  it('sends the CSRF token from the cookie with every request', async () => {
    document.cookie = 'XSRF-TOKEN=token-123'
    const tokens: (string | null)[] = []
    server.use(
      http.get('*/api/scenarios', ({ request }) => {
        tokens.push(request.headers.get('X-XSRF-TOKEN'))
        return HttpResponse.json(page([]))
      }),
    )
    renderApp('/')

    await screen.findByText('No scenarios found.')
    expect(tokens).toEqual(['token-123'])
  })

  it('sends no CSRF header without the cookie', async () => {
    const tokens: (string | null)[] = []
    server.use(
      http.get('*/api/scenarios', ({ request }) => {
        tokens.push(request.headers.get('X-XSRF-TOKEN'))
        return HttpResponse.json(page([]))
      }),
    )
    renderApp('/')

    await screen.findByText('No scenarios found.')
    expect(tokens).toEqual([null])
  })

  it('goes to the Keycloak login when the session is gone', async () => {
    const assign = vi.spyOn(browser, 'assign').mockImplementation(() => undefined)
    server.use(
      http.get('*/api/me', () => new HttpResponse(null, { status: 401 })),
      http.get('*/api/scenarios', () => new HttpResponse(null, { status: 401 })),
    )
    renderApp('/')

    await waitFor(() => expect(assign).toHaveBeenCalledWith(LOGIN_URL))
  })

  it('logs out of the app and of Keycloak', async () => {
    document.cookie = 'XSRF-TOKEN=token-456'
    const assign = vi.spyOn(browser, 'assign').mockImplementation(() => undefined)
    const logoutTokens: (string | null)[] = []
    server.use(
      http.get('*/api/scenarios', () => HttpResponse.json(page([]))),
      http.post('*/logout', ({ request }) => {
        logoutTokens.push(request.headers.get('X-XSRF-TOKEN'))
        return HttpResponse.json({ redirectUrl: 'http://keycloak.localhost:8180/realms/flut/logout?x=1' })
      }),
    )
    const { user } = renderApp('/')

    await user.click(await screen.findByRole('button', { name: 'Log out' }))

    await waitFor(() => expect(assign).toHaveBeenCalledWith('http://keycloak.localhost:8180/realms/flut/logout?x=1'))
    expect(logoutTokens).toEqual(['token-456'])
  })
})

describe('browser', () => {
  it('navigates the whole page', () => {
    browser.assign('#section')

    expect(window.location.hash).toBe('#section')
  })
})
