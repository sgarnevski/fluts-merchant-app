// Aliases for the generated API types (src/api/schema.gen.ts). No hand-written request/response types.
import type { components } from './schema.gen'

type Schemas = components['schemas']

export type ScenarioSummary = Schemas['ScenarioSummary']
export type ScenarioPage = Schemas['ScenarioPage']
export type ScenarioResponse = Schemas['ScenarioResponse']
export type ScenarioRequest = Schemas['ScenarioRequest']
export type ScenariosInput = Schemas['ScenariosInput']
export type EvaluationResponse = Schemas['EvaluationResponse']
export type EvaluationResult = Schemas['EvaluationResult']
export type InputType = ScenarioSummary['inputType']

/** Problem details (RFC 9457) with this API's extension members. */
export type Problem = Schemas['ProblemDetail'] & {
  readonly line?: number
  readonly errors?: readonly string[]
}
export type Me = Schemas['MeResponse']
export type Merchant = Schemas['MerchantResponse']
