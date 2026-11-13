import { execFileSync } from 'node:child_process'
import path from 'node:path'
import { bash, e2eBind, e2eHost } from './support/host'

const script = path.resolve(import.meta.dirname, '../../scripts/e2e-env.sh')

/** Builds the app from the working tree and starts it on an empty database. */
export default function globalSetup() {
  if (process.env.E2E_REUSE === '1') return
  execFileSync(bash(), [script, 'up'], {
    stdio: 'inherit',
    env: { ...process.env, E2E_HOST: e2eHost(), E2E_BIND: e2eBind() },
  })
}
