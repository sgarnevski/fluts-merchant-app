import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, asFileUpload, unwrap } from './client'
import type { InputType, ScenarioRequest, ScenariosInput } from './types'

export type ListParams = {
  readonly page: number
  readonly size: number
  readonly sort: string
  readonly merchant?: string
  readonly inputType?: InputType
  readonly name?: string
}

const scenariosKey = ['scenarios'] as const

export function useScenarioPage(params: ListParams) {
  return useQuery({
    queryKey: [...scenariosKey, 'list', params],
    queryFn: () => unwrap(api.GET('/api/scenarios', { params: { query: { ...params, sort: [params.sort] } } })),
    placeholderData: keepPreviousData,
  })
}

export function useScenario(id: number) {
  return useQuery({
    queryKey: [...scenariosKey, 'detail', id],
    queryFn: () => unwrap(api.GET('/api/scenarios/{id}', { params: { path: { id } } })),
  })
}

/** Mutations change stored scenarios, so every cached scenario query is refreshed afterwards. */
function useScenarioMutation<TInput, TResult>(mutationFn: (input: TInput) => Promise<TResult>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: scenariosKey }),
  })
}

export function useCreateScenario() {
  return useScenarioMutation((scenario: ScenarioRequest) => unwrap(api.POST('/api/scenarios', { body: scenario })))
}

export function useUpdateScenario(id: number) {
  return useScenarioMutation((scenario: ScenarioRequest) =>
    unwrap(api.PUT('/api/scenarios/{id}', { params: { path: { id } }, body: scenario })),
  )
}

export function useDeleteScenario() {
  return useScenarioMutation((id: number) => unwrap(api.DELETE('/api/scenarios/{id}', { params: { path: { id } } })))
}

export function useImportFile() {
  return useScenarioMutation((file: File) => unwrap(api.POST('/api/scenarios/import', asFileUpload(file))))
}

export type EvaluationInput =
  | { readonly kind: 'json'; readonly input: ScenariosInput }
  | { readonly kind: 'file'; readonly file: File }

/** Evaluations are stored by flut-app too, hence also a scenario mutation. */
export function useEvaluate() {
  return useScenarioMutation((evaluation: EvaluationInput) =>
    evaluation.kind === 'json'
      ? unwrap(api.POST('/api/evaluations', { body: evaluation.input }))
      : unwrap(api.POST('/api/evaluations', asFileUpload(evaluation.file))),
  )
}
