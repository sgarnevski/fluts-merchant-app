import { useQuery } from '@tanstack/react-query'
import { browser } from '../lib/browser'
import { api, csrfHeaders, unwrap } from './client'

/** The logged-in user; loaded once per page load. */
export function useMe() {
  return useQuery({ queryKey: ['me'], queryFn: () => unwrap(api.GET('/api/me')), staleTime: Infinity })
}

/** Whether the user may change data (role `editor`); false while unknown. */
export function useCanEdit(): boolean {
  return useMe().data?.roles.includes('editor') ?? false
}

/** Whether the user sees every merchant (role `admin`); false while unknown. */
export function useIsAdmin(): boolean {
  return useMe().data?.roles.includes('admin') ?? false
}

/** The registered merchants (for the admin's filter and the merchant column). */
export function useMerchants() {
  return useQuery({ queryKey: ['merchants'], queryFn: () => unwrap(api.GET('/api/merchants')), staleTime: Infinity })
}

/**
 * Ends the session in flut-app, then lets the browser visit the Keycloak end-session URL flut-app
 * answers with, so Keycloak logs out too and sends the browser back to the app.
 */
export async function logout(): Promise<void> {
  const response = await fetch(`${window.location.origin}/logout`, { method: 'POST', headers: csrfHeaders() })
  const { redirectUrl } = (await response.json()) as { redirectUrl: string }
  browser.assign(redirectUrl)
}
