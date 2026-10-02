import { describe, expect, it } from 'vitest'
import { ApiError, asFileUpload, unwrap } from './client'

const response = (status: number) => new Response(null, { status })

describe('unwrap', () => {
  it('returns the data of a successful response', async () => {
    await expect(unwrap(Promise.resolve({ data: 42, response: response(200) }))).resolves.toBe(42)
  })

  it('throws the problem details of an error response', async () => {
    const failure = unwrap(Promise.resolve({ error: { detail: 'Bad pile', line: 2 }, response: response(400) }))

    await expect(failure).rejects.toEqual(new ApiError(400, { detail: 'Bad pile', line: 2 }))
    await expect(failure).rejects.toHaveProperty('status', 400)
  })

  it('throws even when the error response has no body', async () => {
    await expect(unwrap(Promise.resolve({ response: response(502) }))).rejects.toThrow('Request failed with status 502')
  })
})

describe('ApiError', () => {
  it('prefers the detail, then the title for its message', () => {
    expect(new ApiError(400, { detail: 'detail', title: 'title' }).message).toBe('detail')
    expect(new ApiError(404, { title: 'Not Found' }).message).toBe('Not Found')
  })
})

describe('asFileUpload', () => {
  it('sends the file as multipart part "file"', () => {
    const file = new File(['1\n1 5\n'], 'input.txt')

    const form = asFileUpload(file).bodySerializer()

    expect(form.get('file')).toBe(file)
  })
})
