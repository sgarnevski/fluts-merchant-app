import { useNavigate, useParams } from 'react-router'
import { useScenario, useUpdateScenario } from '../api/queries'
import { ErrorMessage } from '../components/ErrorMessage'
import { ScenarioForm } from './ScenarioForm'

export function EditScenarioPage() {
  const id = Number(useParams().id)
  const scenario = useScenario(id)
  const update = useUpdateScenario(id)
  const navigate = useNavigate()

  if (scenario.isPending) {
    return <p className="muted">Loading…</p>
  }
  if (scenario.isError) {
    return <ErrorMessage error={scenario.error} />
  }
  return (
    <>
      <h1>Edit “{scenario.data.name}”</h1>
      <ScenarioForm
        initial={{
          name: scenario.data.name,
          schuurs: scenario.data.schuurs.map((schuur) => schuur.boxPrices.join(' ')),
        }}
        submitLabel="Save changes"
        pending={update.isPending}
        error={update.error}
        onSubmit={(request) => update.mutate(request, { onSuccess: () => navigate(`/scenarios/${id}`) })}
      />
    </>
  )
}
