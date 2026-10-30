import { randomBytes } from 'node:crypto'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'

const envPath = '.env'
const envContent = existsSync(envPath) ? readFileSync(envPath, 'utf8') : ''
const secretEntries = [...envContent.matchAll(/^[ \t]*AUTH_TOKEN_SECRET[ \t]*=(.*)$/gm)]

if (secretEntries.some(([, value]) => value.trim() !== '')) {
  console.log('AUTH_TOKEN_SECRET is already set in .env; leaving it unchanged.')
  process.exit(0)
}

const secret = randomBytes(32).toString('base64')
const emptyEntry = /^[ \t]*AUTH_TOKEN_SECRET[ \t]*=.*$/m
const updatedContent = emptyEntry.test(envContent)
  ? envContent.replace(emptyEntry, `AUTH_TOKEN_SECRET=${secret}`)
  : `${envContent}${envContent && !envContent.endsWith('\n') ? '\n' : ''}AUTH_TOKEN_SECRET=${secret}\n`

writeFileSync(envPath, updatedContent, { mode: 0o600 })
console.log('Generated a 256-bit AUTH_TOKEN_SECRET in .env.')
