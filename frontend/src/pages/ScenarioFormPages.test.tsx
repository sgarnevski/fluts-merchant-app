import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { ScenarioRequest } from '../api/types'
import { example2 } from '../test/fixtures'
import { renderApp } from '../test/render'
import { server } from '../test/server'

function detailReturning(scenario = example2) {
  server.use(http.get('*/api/scenarios/:id', () => HttpResponse.json(scenario)))
}

describe('NewScenarioPage', () => {
  it('creates a scenario and opens it', async () => {
    const created: ScenarioRequest[] = []
    server.use(
      http.post('*/api/scenarios', async ({ request }) => {
        created.push((await request.json()) as ScenarioRequest)
        return HttpResponse.json(example2, { status: 201 })
      }),
    )
    detailReturning()
    const { user, router } = renderApp('/scenarios/new')

    await user.type(screen.getByLabelText('Name'), '  Example 2 ')
    await user.type(screen.getByLabelText('Schuur 1 box prices'), '7 3, 11 9 10')
    await user.click(screen.getByRole('button', { name: 'Add schuur' }))
    await user.click(screen.getByRole('button', { name: 'Add schuur' }))
    await user.type(screen.getByLabelText('Schuur 3 box prices'), '1 2 3')
    await user.click(screen.getByRole('button', { name: 'Remove schuur 2' }))
    await user.click(screen.getByRole('button', { name: 'Create scenario' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/scenarios/7'))
    expect(created).toEqual([{ name: 'Example 2', schuurs: [{ boxPrices: [7, 3, 11, 9, 10] }, { boxPrices: [1, 2, 3] }] }])
  })

  it('keeps an empty schuur as an empty pile', async () => {
    const created: ScenarioRequest[] = []
    server.use(
      http.post('*/api/scenarios', async ({ request }) => {
        created.push((await request.json()) as ScenarioRequest)
        return HttpResponse.json(example2, { status: 201 })
      }),
    )
    detailReturning()
    const { user } = renderApp('/scenarios/new')

    await user.type(screen.getByLabelText('Name'), 'Empty')
    await user.click(screen.getByRole('button', { name: 'Create scenario' }))

    await waitFor(() => expect(created).toEqual([{ name: 'Empty', schuurs: [{ boxPrices: [] }] }]))
  })

  it('rejects prices that are not positive whole numbers before sending', async () => {
    const { user } = renderApp('/scenarios/new')
    expect(screen.getByRole('button', { name: 'Remove schuur 1' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Add schuur' }))
    await user.type(screen.getByLabelText('Schuur 2 box prices'), '4 0')
    await user.click(screen.getByRole('button', { name: 'Create scenario' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Schuur 2: "0" is not a positive whole number')
  })

  it('shows every field the server rejects', async () => {
    server.use(
      http.post('*/api/scenarios', () =>
        HttpResponse.json({ detail: 'Invalid request content', errors: ['name: must not be blank'] }, { status: 400 }),
      ),
    )
    const { user } = renderApp('/scenarios/new')

    await user.click(screen.getByRole('button', { name: 'Create scenario' }))

    expect(await screen.findByText('name: must not be blank')).toBeInTheDocument()
  })
})

describe('EditScenarioPage', () => {
  it('loads the scenario into the form and saves the changes', async () => {
    detailReturning()
    const saved: ScenarioRequest[] = []
    server.use(
      http.put('*/api/scenarios/:id', async ({ request }) => {
        saved.push((await request.json()) as ScenarioRequest)
        return HttpResponse.json({ ...example2, name: 'Renamed' })
      }),
    )
    const { user, router } = renderApp('/scenarios/7/edit')

    expect(screen.getByText('Loading…')).toBeInTheDocument()
    const name = await screen.findByLabelText('Name')
    expect(screen.getByRole('heading', { name: 'Edit “Example 2”' })).toBeInTheDocument()
    expect(screen.getByLabelText('Schuur 1 box prices')).toHaveValue('7 3 11 9 10')
    await user.clear(name)
    await user.type(name, 'Renamed')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/scenarios/7'))
    expect(saved[0]).toEqual({
      name: 'Renamed',
      schuurs: [{ boxPrices: [7, 3, 11, 9, 10] }, { boxPrices: [] }, { boxPrices: [1, 2, 3, 4, 10, 16, 10, 4, 16] }],
    })
  })

  it('reports a scenario that does not exist', async () => {
    server.use(http.get('*/api/scenarios/:id', () => HttpResponse.json({ detail: 'Scenario 99 not found' }, { status: 404 })))
    renderApp('/scenarios/99/edit')

    expect(await screen.findByRole('alert')).toHaveTextContent('Scenario 99 not found')
  })
})
