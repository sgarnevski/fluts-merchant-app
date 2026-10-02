import { QueryClient } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter } from 'react-router'
import { App, routes } from './App'
import './styles/global.css'

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: 1 } } })
const router = createBrowserRouter(routes)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App queryClient={queryClient} router={router} />
  </StrictMode>,
)
