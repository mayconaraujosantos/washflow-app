import os from 'node:os'

const interfaces = os.networkInterfaces()
const candidates = []

for (const name of Object.keys(interfaces)) {
  for (const net of interfaces[name] ?? []) {
    if (net.family === 'IPv4' && !net.internal) {
      candidates.push({ name, address: net.address })
    }
  }
}

if (candidates.length === 0) {
  console.log('No LAN IPv4 address found.')
} else {
  for (const { name, address } of candidates) {
    console.log(`${address}  (${name})`)
  }
}
