import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      ...tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
    },
    rules: {
      // `export namespace Foo { export type Model = ... }` alongside
      // `export interface Foo` attaches a usecase's return type to its own
      // contract (clean-ts-api/clean-react convention) — not the ES2015
      // namespace pattern this rule otherwise guards against.
      '@typescript-eslint/no-namespace': 'off',
    },
  },
])
