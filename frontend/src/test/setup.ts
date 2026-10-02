import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { Blob as NodeBlob, File as NodeFile } from 'node:buffer'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

// jsdom brings its own FormData / File / Blob, which Node's fetch (used by the mock server) cannot
// send as a request body. Use Node's classes instead; Node exposes its FormData only via Response.
const nodeFormData = await new Response('', {
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
}).formData()
for (const [name, value] of Object.entries({ FormData: nodeFormData.constructor, File: NodeFile, Blob: NodeBlob })) {
  Object.defineProperty(globalThis, name, { value, writable: true, configurable: true })
}

// Generous waits, so the tests stay stable on a busy machine or CI runner.
configure({ asyncUtilTimeout: 3000 })

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => {
  cleanup()
  server.resetHandlers()
})
afterAll(() => server.close())
