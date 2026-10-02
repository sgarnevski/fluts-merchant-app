import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { RouterProvider, type createBrowserRouter, type RouteObject } from 'react-router'
import { Layout } from './components/Layout'
import { EditScenarioPage } from './pages/EditScenarioPage'
import { EvaluatorPage } from './pages/EvaluatorPage'
import { NewScenarioPage } from './pages/NewScenarioPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ScenarioDetailPage } from './pages/ScenarioDetailPage'
import { ScenarioListPage } from './pages/ScenarioListPage'
import { SignInFailedPage } from './pages/SignInFailedPage'

export const routes: RouteObject[] = [
  { path: '/sign-in-failed', element: <SignInFailedPage /> },
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <ScenarioListPage /> },
      { path: 'scenarios/new', element: <NewScenarioPage /> },
      { path: 'scenarios/:id', element: <ScenarioDetailPage /> },
      { path: 'scenarios/:id/edit', element: <EditScenarioPage /> },
      { path: 'evaluate', element: <EvaluatorPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export type AppRouter = ReturnType<typeof createBrowserRouter>

export function App({ queryClient, router }: { readonly queryClient: QueryClient; readonly router: AppRouter }) {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}
