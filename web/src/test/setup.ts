import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'

// `test.globals` is off (explicit `vitest` imports everywhere else), so
// Testing Library's usual auto-cleanup-via-global-afterEach doesn't kick in
// on its own - without this, DOM from one test's render() leaks into the
// next in the same file.
afterEach(cleanup)
