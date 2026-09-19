import { HealthModel } from '@/domain/models/health-model'

export interface LoadHealthStatus {
  load: () => Promise<LoadHealthStatus.Model>
}

export namespace LoadHealthStatus {
  export type Model = HealthModel
}
