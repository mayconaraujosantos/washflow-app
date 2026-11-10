import { execFileSync } from 'node:child_process'
import path from 'node:path'

const script = path.resolve(import.meta.dirname, '../../scripts/e2e-env.sh')

/** Builds the app from the working tree and starts it on an empty database. */
export default function globalSetup() {
  if (process.env.E2E_REUSE === '1') return
  execFileSync(script, ['up'], { stdio: 'inherit' })
}
