import { execFileSync } from 'node:child_process'
import path from 'node:path'
import { bash } from './support/host'

const script = path.resolve(import.meta.dirname, '../../scripts/e2e-env.sh')

/** Removes the throwaway environment unless E2E_KEEP=1 (to look around after). */
export default function globalTeardown() {
  if (process.env.E2E_KEEP === '1' || process.env.E2E_REUSE === '1') return
  execFileSync(bash(), [script, 'down'], { stdio: 'inherit' })
}
