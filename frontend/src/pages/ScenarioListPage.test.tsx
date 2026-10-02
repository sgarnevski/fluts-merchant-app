import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { page, summary } from '../test/fixtures'
import { renderApp } from '../test/render'
import { server } from '../test/server'

/** Answers the list endpoint and records the query of every request. */
function listReturning(...pages: ReturnType<typeof page>[]) {
  const queries: URLSearchParams[] = []
  server.use(
    http.get('*/api/scenarios', ({ request }) => {
      const query = new URL(request.url).searchParams
      queries.push(query)
      return HttpResponse.json(pages[Math.min(Number(query.get('page')), pages.length - 1)])
    }),
  )
  return queries
}

describe('ScenarioListPage', () => {
  it('lists scenarios with their computed maximum profit', async () => {
    const queries = listReturning(page([summary(), summary({ id: 8, name: 'Imported', inputType: 'FILE', maxProfit: 8 })]))
    renderApp('/')

    expect(screen.getByText('Loading…')).toBeInTheDocument()
    const rows = await screen.findAllByRole('row')
    expect(rows.slice(1).map((row) => row.textContent)).toEqual([
      expect.stringContaining('Example 2JSON240'),
      expect.stringContaining('ImportedFILE28'),
    ])
    expect(screen.getByRole('link', { name: 'Example 2' })).toHaveAttribute('href', '/scenarios/7')
    expect(queries[0].toString()).toBe('page=0&size=20&sort=createdAt%2Cdesc')
  })

  it('filters by name and input type and sorts', async () => {
    const queries = listReturning(page([summary()]))
    const { user } = renderApp('/')
    await screen.findByRole('link', { name: 'Example 2' })

    await user.type(screen.getByLabelText('Search by name'), 'ex')
    await user.selectOptions(screen.getByLabelText('Input type'), 'KAFKA')
    await user.selectOptions(screen.getByLabelText('Sort'), 'name,asc')

    await waitFor(() =>
      expect(queries.at(-1)?.toString()).toBe('page=0&size=20&sort=name%2Casc&inputType=KAFKA&name=ex'),
    )
  })

  it('says when nothing matches', async () => {
    listReturning(page([]))
    renderApp('/')

    expect(await screen.findByText('No scenarios found.')).toBeInTheDocument()
  })

  it('pages through the results', async () => {
    const queries = listReturning(page([summary()], 0, 2), page([summary({ id: 9, name: 'Second page' })], 1, 2))
    const { user } = renderApp('/')
    await screen.findByText('Page 1 of 2')
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Next' }))
    expect(await screen.findByRole('link', { name: 'Second page' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Previous' }))
    expect(await screen.findByText('Page 1 of 2')).toBeInTheDocument()
    expect(queries.map((query) => query.get('page'))).toEqual(['0', '1', '0'])
  })

  it('deletes a scenario after confirmation', async () => {
    listReturning(page([summary()]))
    const deleted: string[] = []
    server.use(
      http.delete('*/api/scenarios/:id', ({ params }) => {
        deleted.push(String(params.id))
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true)
    const { user } = renderApp('/')
    const row = (await screen.findByRole('link', { name: 'Example 2' })).closest('tr') as HTMLElement

    await user.click(within(row).getByRole('button', { name: 'Delete' }))
    await user.click(within(row).getByRole('button', { name: 'Delete' }))

    expect(confirm).toHaveBeenCalledWith('Delete "Example 2"?')
    await waitFor(() => expect(deleted).toEqual(['7']))
  })

  it('shows why a delete failed', async () => {
    listReturning(page([summary()]))
    server.use(http.delete('*/api/scenarios/:id', () => HttpResponse.json({ detail: 'Scenario 7 not found' }, { status: 404 })))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { user } = renderApp('/')

    await user.click(await screen.findByRole('button', { name: 'Delete' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Scenario 7 not found')
  })

  it('shows why the list could not be loaded', async () => {
    server.use(http.get('*/api/scenarios', () => HttpResponse.json({ detail: "Cannot sort by 'x'" }, { status: 400 })))
    renderApp('/')

    expect(await screen.findByRole('alert')).toHaveTextContent("Cannot sort by 'x'")
  })

  it('imports a file', async () => {
    listReturning(page([]))
    const uploads: string[] = []
    server.use(
      http.post('*/api/scenarios/import', async ({ request }) => {
        uploads.push(((await request.formData()).get('file') as File).name)
        return HttpResponse.json({ results: [{ id: 1, scenario: 'Scenario 1', maxProfit: 8, flutCounts: [4] }] }, { status: 201 })
      }),
    )
    const { user } = renderApp('/')

    await user.upload(await screen.findByLabelText('Import file…'), new File(['1\n6 12 3 10 7 16 5\n0\n'], 'input.txt'))

    expect(await screen.findByText('Imported 1 scenario(s).')).toBeInTheDocument()
    expect(uploads).toEqual(['input.txt'])
  })

  it('shows why an import failed', async () => {
    listReturning(page([]))
    server.use(
      http.post('*/api/scenarios/import', () =>
        HttpResponse.json({ detail: 'Schuur declares 2 boxes but lists 1 prices', line: 2 }, { status: 400 }),
      ),
    )
    const { user } = renderApp('/')

    await user.upload(await screen.findByLabelText('Import file…'), new File(['1\n2 5\n'], 'bad.txt'))

    expect(await screen.findByRole('alert')).toHaveTextContent('Line 2: Schuur declares 2 boxes but lists 1 prices')
  })
})
