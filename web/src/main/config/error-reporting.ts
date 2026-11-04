import { ErrorReporter } from '@/infra/observability/error-reporter'
import { env } from './env'

export const errorReporter = new ErrorReporter(
  `${env.apiUrl}/api/client-errors`,
)
