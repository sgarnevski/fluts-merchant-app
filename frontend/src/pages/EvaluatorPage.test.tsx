import { fireEvent, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { ScenariosInput } from '../api/types'
import { renderApp } from '../test/render'
import { server } from '../test/server'

const RESULTS = {
  results: [
    { id: 1, scenario: 'Example 1', maxProfit: 8, flutCounts: [4] },
    { id: 2, scenario: 'Example 2', maxProfit: 40, flutCounts: [6, 7, 8, 9, 10, 12, 13] },
  ],
}

describe('EvaluatorPage', () => {
  it('evaluates the JSON example and shows both output formats', async () => {
    const sent: ScenariosInput[] = []
    server.use(
      http.post('*/api/evaluations', async ({ request }) => {
        sent.push((await request.json()) as ScenariosInput)
        return HttpResponse.json(RESULTS)
      }),
    )
    const { user } = renderApp('/evaluate')

    await user.click(screen.getByRole('button', { name: 'Evaluate JSON' }))

    expect(await screen.findByRole('link', { name: 'Example 2' })).toHaveAttribute('href', '/scenarios/2')
    expect(screen.getByText(/Number of fluts to buy: 6 7 8 9 10 12 13/)).toBeInTheDocument()
    expect(sent[0].scenarios).toHaveLength(2)
  })

  it('evaluates an uploaded file', async () => {
    const uploads: string[] = []
    server.use(
      http.post('*/api/evaluations', async ({ request }) => {
        uploads.push(((await request.formData()).get('file') as File).name)
        return HttpResponse.json(RESULTS)
      }),
    )
    const { user } = renderApp('/evaluate')

    await user.upload(screen.getByLabelText('Evaluate a file…'), new File(['1\n6 12 3 10 7 16 5\n0\n'], 'input.txt'))

    expect(await screen.findByText('Results')).toBeInTheDocument()
    expect(uploads).toEqual(['input.txt'])
  })

  it('rejects text that is not JSON before sending', async () => {
    const { user } = renderApp('/evaluate')

    fireEvent.change(screen.getByLabelText('Scenarios as JSON'), { target: { value: '{ nope' } })
    await user.click(screen.getByRole('button', { name: 'Evaluate JSON' }))

    expect(screen.getByRole('alert')).toHaveTextContent(/^Invalid JSON: /)
  })

  it('shows why the server rejected the input', async () => {
    server.use(http.post('*/api/evaluations', () => HttpResponse.json({ detail: 'No scenarios given' }, { status: 400 })))
    const { user } = renderApp('/evaluate')

    fireEvent.change(screen.getByLabelText('Scenarios as JSON'), { target: { value: '{"scenarios": []}' } })
    await user.click(screen.getByRole('button', { name: 'Evaluate JSON' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No scenarios given')
  })
})
