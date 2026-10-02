import { useNavigate } from 'react-router'
import { useCreateScenario } from '../api/queries'
import { ScenarioForm } from './ScenarioForm'

export function NewScenarioPage() {
  const creation = useCreateScenario()
  const navigate = useNavigate()
  return (
    <>
      <h1>New scenario</h1>
      <ScenarioForm
        initial={{ name: '', schuurs: [''] }}
        submitLabel="Create scenario"
        pending={creation.isPending}
        error={creation.error}
        onSubmit={(request) =>
          creation.mutate(request, { onSuccess: (created) => navigate(`/scenarios/${created.id}`) })
        }
      />
    </>
  )
}
