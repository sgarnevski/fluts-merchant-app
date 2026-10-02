import createClient from 'openapi-fetch'
import { browser } from '../lib/browser'
import type { paths } from './schema.gen'
import type { Problem } from './types'

/** Where flut-app starts the Keycloak login. */
export const LOGIN_URL = '/oauth2/authorization/keycloak'

/** The CSRF token flut-app puts in the readable `XSRF-TOKEN` cookie; sent back as `X-XSRF-TOKEN` header. */
export function csrfHeaders(): Record<string, string> {
  const cookie = document.cookie.split('; ').find((entry) => entry.startsWith('XSRF-TOKEN='))
  return cookie === undefined ? {} : { 'X-XSRF-TOKEN': decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) }
}

/**
 * Typed client for the flut-app API; always same-origin (dev: Vite proxy, prod: nginx).
 * `fetch` is looked up per request, so a fetch installed later (e.g. by a test mock server) is used.
 */
export const api = createClient<paths, 'application/json'>({
  baseUrl: window.location.origin,
  fetch: (request) => globalThis.fetch(request),
})

api.use({
  onRequest({ request }) {
    Object.entries(csrfHeaders()).forEach(([name, value]) => request.headers.set(name, value))
    return request
  },
  onResponse({ response }) {
    if (response.status === 401) {
      browser.assign(LOGIN_URL)
    }
    return response
  },
})

/** A failed API call, carrying the problem details sent by the server. */
export class ApiError extends Error {
  readonly status: number
  readonly problem: Problem

  constructor(status: number, problem: Problem) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

type FetchResult<T> = { data?: T; error?: unknown; response: Response }

/** Returns the response data, or throws an {@link ApiError} for an error response. */
export async function unwrap<T>(request: Promise<FetchResult<T>>): Promise<T> {
  const { data, error, response } = await request
  if (!response.ok) {
    throw new ApiError(response.status, (error ?? {}) as Problem)
  }
  return data as T
}

/** Request options that send `file` as multipart form data (part name `file`). */
export function asFileUpload(file: File) {
  const form = new FormData()
  form.append('file', file)
  return { body: { file: file.name }, bodySerializer: () => form }
}
