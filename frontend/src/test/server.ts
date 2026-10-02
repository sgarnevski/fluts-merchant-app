import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import type { Me } from '../api/types'

const DUTCH = { code: 'dutch', name: 'Dutch merchant' }
const FRENCH = { code: 'french', name: 'French merchant' }

export const EDITOR: Me = { username: 'dutch', roles: ['editor', 'viewer'], merchant: DUTCH }
export const VIEWER: Me = { username: 'viewer', roles: ['viewer'], merchant: DUTCH }
export const ADMIN: Me = { username: 'admin', roles: ['admin'] }

/** Mock API: logged in as the Dutch editor by default; each test installs what else it needs with server.use(...). */
export const server = setupServer(
  http.get('*/api/me', () => HttpResponse.json(EDITOR)),
  http.get('*/api/merchants', () => HttpResponse.json([DUTCH, FRENCH])),
)
