import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { page } from './test/fixtures'
import { renderApp } from './test/render'
import { server } from './test/server'

describe('App', () => {
  it('navigates between the pages', async () => {
    server.use(http.get('*/api/scenarios', () => HttpResponse.json(page([]))))
    const { user } = renderApp('/')

    await user.click(await screen.findByRole('link', { name: 'Evaluate' }))
    expect(screen.getByRole('heading', { name: 'Evaluate' })).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Trading of Fluts' }))
    expect(await screen.findByText('No scenarios found.')).toBeInTheDocument()
  })

  it('explains a failed sign-in without asking the server who is logged in', () => {
    const me = vi.fn()
    server.use(http.get('*/api/me', () => {
      me()
      return new HttpResponse(null, { status: 401 })
    }))
    renderApp('/sign-in-failed')

    expect(screen.getByRole('heading', { name: 'Sign-in could not be completed' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Log in again' })).toHaveAttribute('href', '/oauth2/authorization/keycloak')
    expect(me).not.toHaveBeenCalled()
  })

  it('shows a not-found page for unknown paths', async () => {
    const { user } = renderApp('/nowhere')

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    server.use(http.get('*/api/scenarios', () => HttpResponse.json(page([]))))
    await user.click(screen.getByRole('link', { name: 'Back to the scenarios' }))
    expect(await screen.findByRole('heading', { name: 'Scenarios' })).toBeInTheDocument()
  })
})
