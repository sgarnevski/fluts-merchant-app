import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { example2, page } from '../test/fixtures'
import { renderApp } from '../test/render'
import { server } from '../test/server'

describe('ScenarioDetailPage', () => {
  it('shows the result and draws every pile', async () => {
    server.use(http.get('*/api/scenarios/:id', () => HttpResponse.json(example2)))
    renderApp('/scenarios/7')

    expect(screen.getByText('Loading…')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Example 2' })).toBeInTheDocument()
    expect(screen.getByText('40 florins')).toBeInTheDocument()
    expect(screen.getByText('6 7 8 9 10 12 13')).toBeInTheDocument()
    expect(screen.getAllByRole('figure')).toHaveLength(3)
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/scenarios/7/edit')
    expect(screen.getByRole('link', { name: 'Download scenario' })).toHaveAttribute('href', '/api/scenarios/7/file')
    expect(screen.getAllByRole('link', { name: 'Download result' }).map((link) => link.getAttribute('href'))).toEqual([
      '/api/scenarios/7/result/file',
      '/api/scenarios/7/schuurs/1/result/file',
      '/api/scenarios/7/schuurs/2/result/file',
      '/api/scenarios/7/schuurs/3/result/file',
    ])
  })

  it('shows five schuurs per page', async () => {
    const schuurs = Array.from({ length: 9 }, (_, index) => ({ boxPrices: [index + 1], maxProfit: 9 - index, flutCounts: [1] }))
    server.use(http.get('*/api/scenarios/:id', () => HttpResponse.json({ ...example2, schuurs })))
    const { user } = renderApp('/scenarios/7')

    expect(await screen.findByText('Schuurs 1–5 of 9')).toBeInTheDocument()
    expect(screen.getAllByRole('figure').map((figure) => figure.getAttribute('aria-label')))
      .toEqual(['Schuur 1', 'Schuur 2', 'Schuur 3', 'Schuur 4', 'Schuur 5'])
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(screen.getByText('Schuurs 6–9 of 9')).toBeInTheDocument()
    expect(screen.getByRole('figure', { name: 'Schuur 9' })).toHaveTextContent('9')
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Previous' }))
    expect(screen.getByText('Schuurs 1–5 of 9')).toBeInTheDocument()
  })

  it('reports a scenario that does not exist', async () => {
    server.use(http.get('*/api/scenarios/:id', () => HttpResponse.json({ detail: 'Scenario 99 not found' }, { status: 404 })))
    renderApp('/scenarios/99')

    expect(await screen.findByRole('alert')).toHaveTextContent('Scenario 99 not found')
  })

  it('deletes the scenario after confirmation and goes back to the list', async () => {
    server.use(
      http.get('*/api/scenarios/:id', () => HttpResponse.json(example2)),
      http.delete('*/api/scenarios/:id', () => new HttpResponse(null, { status: 204 })),
      http.get('*/api/scenarios', () => HttpResponse.json(page([]))),
    )
    vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true)
    const { user, router } = renderApp('/scenarios/7')

    await user.click(await screen.findByRole('button', { name: 'Delete' }))
    expect(router.state.location.pathname).toBe('/scenarios/7')
    await user.click(screen.getByRole('button', { name: 'Delete' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
  })

  it('shows why a delete failed', async () => {
    server.use(
      http.get('*/api/scenarios/:id', () => HttpResponse.json(example2)),
      http.delete('*/api/scenarios/:id', () => HttpResponse.json({ title: 'Forbidden' }, { status: 403 })),
    )
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { user } = renderApp('/scenarios/7')

    await user.click(await screen.findByRole('button', { name: 'Delete' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Forbidden')
  })
})
