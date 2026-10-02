import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { ErrorMessage } from './ErrorMessage'
import { FilePicker } from './FilePicker'
import { Pile } from './Pile'
import { ResultsTable } from './ResultsTable'

describe('ErrorMessage', () => {
  it('shows the line number of invalid input', () => {
    render(<ErrorMessage error={new ApiError(400, { detail: 'Schuur declares 3 boxes but lists 2 prices', line: 2 })} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Line 2: Schuur declares 3 boxes but lists 2 prices')
  })

  it('lists every invalid field', () => {
    render(<ErrorMessage error={new ApiError(400, { detail: 'Invalid request content', errors: ['name: must not be blank'] })} />)

    expect(screen.getByRole('listitem')).toHaveTextContent('name: must not be blank')
  })

  it('shows the message of other errors', () => {
    render(<ErrorMessage error={new Error('Network down')} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Network down')
  })

  it('has a fallback for anything else', () => {
    render(<ErrorMessage error="???" />)

    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong')
  })
})

describe('Pile', () => {
  it('draws the boxes top first, coloured by profit per flut, and outlines the ones to buy', () => {
    render(<Pile title="Schuur 1" schuur={{ boxPrices: [3, 10, 12], maxProfit: 7, flutCounts: [1, 2] }} fileUrl="/f" resultUrl="/r" />)

    const boxes = screen.getAllByRole('listitem')
    expect(boxes.map((box) => box.textContent)).toEqual(['3', '10', '12'])
    expect(boxes[0].className).toMatch(/profit/)
    expect(boxes[0].className).toMatch(/bought/)
    expect(boxes[1].className).toMatch(/even/)
    expect(boxes[1].className).not.toMatch(/bought/)
    expect(boxes[2].className).toMatch(/loss/)
    expect(screen.getByText('Buy the top 1 · +7 florins (or 2)')).toBeInTheDocument()
  })

  it('says when nothing should be bought', () => {
    render(<Pile title="Schuur 3" schuur={{ boxPrices: [16, 11], maxProfit: 0, flutCounts: [0] }} fileUrl="/f" resultUrl="/r" />)

    expect(screen.getAllByRole('listitem').every((box) => !box.className.includes('bought'))).toBe(true)
    expect(screen.getByText('Buy nothing')).toBeInTheDocument()
  })

  it('draws at most 20 boxes and says how many more there are', () => {
    const prices = Array.from({ length: 54 }, (_, index) => (index % 2 === 0 ? 1 : 40))
    render(<Pile title="Schuur 1" schuur={{ boxPrices: prices, maxProfit: 113, flutCounts: [49, 50] }} fileUrl="/api/scenarios/7/schuurs/1/file" resultUrl="/api/scenarios/7/schuurs/1/result/file" />)

    expect(screen.getAllByRole('listitem')).toHaveLength(20)
    expect(screen.getByText(/… and 34 more/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'see the file' })).toHaveAttribute('href', '/api/scenarios/7/schuurs/1/file')
    expect(screen.getByRole('link', { name: 'Download result' })).toHaveAttribute('href', '/api/scenarios/7/schuurs/1/result/file')
    expect(screen.getByText('Buy the top 49 · +113 florins (or 50)')).toBeInTheDocument()
  })

  it('marks an empty pile', () => {
    render(<Pile title="Schuur 2" schuur={{ boxPrices: [], maxProfit: 0, flutCounts: [0] }} fileUrl="/f" resultUrl="/r" />)

    expect(screen.getByRole('figure', { name: 'Schuur 2' })).toHaveTextContent('empty')
    expect(screen.getByRole('figure', { name: 'Schuur 2' })).toHaveTextContent('Buy nothing')
  })
})

describe('ResultsTable', () => {
  it('links saved scenarios and lists the flut counts', () => {
    render(
      <MemoryRouter>
        <ResultsTable
          results={[
            { id: 5, scenario: 'Saved', maxProfit: 8, flutCounts: [4] },
            { scenario: 'Not saved', maxProfit: 0, flutCounts: [0, 1] },
          ]}
        />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Saved' })).toHaveAttribute('href', '/scenarios/5')
    expect(screen.queryByRole('link', { name: 'Not saved' })).toBeNull()
    expect(screen.getByText('0 1')).toBeInTheDocument()
  })
})

describe('FilePicker', () => {
  it('hands over the picked file', async () => {
    const onFile = vi.fn()
    render(<FilePicker label="Pick" onFile={onFile} />)
    const file = new File(['0'], 'input.txt')

    await userEvent.upload(screen.getByLabelText('Pick'), file)

    expect(onFile).toHaveBeenCalledWith(file)
  })

  it('ignores a cancelled pick', () => {
    const onFile = vi.fn()
    render(<FilePicker label="Pick" onFile={onFile} />)
    const input = screen.getByLabelText('Pick')

    fireEvent.change(input, { target: { files: [] } })
    Object.defineProperty(input, 'files', { value: null })
    fireEvent.change(input)

    expect(onFile).not.toHaveBeenCalled()
  })
})
