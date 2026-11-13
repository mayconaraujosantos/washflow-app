// Fake Evolution API for the e2e pod (scripts/e2e-env.sh): the endpoints the app
// calls (instance state, create, webhook, QR code, logout, sendText), checked
// against the global apikey like the real server, plus /test/* endpoints the
// Playwright scenario uses to "scan" the QR code, write as a customer (posting
// the webhook to the app exactly like Evolution would) and read what the app
// sent. Plain JavaScript run with `bun -e`, so no file has to be mounted.

const API_KEY = process.env.EVOLUTION_API_KEY
const PORT = Number(process.env.FAKE_EVOLUTION_PORT ?? 8080)
// A valid 1x1 PNG: the manager screen only needs an image to show.
const QR =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII='

/** name -> { state: 'close' | 'connecting' | 'open', webhook } */
const instances = new Map()
/** Every text the app sent: { instance, number, text } */
const sent = []
let received = 0

const json = (body, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })

async function evolution(request, parts) {
  if (request.headers.get('apikey') !== API_KEY) {
    return json({ status: 401, error: 'Unauthorized' }, 401)
  }
  const [area, action, name] = parts
  const method = request.method
  const instance = name ? instances.get(name) : undefined

  if (area === 'instance' && action === 'create' && method === 'POST') {
    const body = await request.json()
    instances.set(body.instanceName, { state: 'close', webhook: body.webhook })
    return json(
      { instance: { instanceName: body.instanceName, status: 'created' } },
      201,
    )
  }
  if (!instance) {
    return json(
      {
        status: 404,
        error: 'Not Found',
        response: { message: ['instance does not exist'] },
      },
      404,
    )
  }
  if (area === 'instance' && action === 'connectionState' && method === 'GET') {
    return json({ instance: { instanceName: name, state: instance.state } })
  }
  if (area === 'instance' && action === 'connect' && method === 'GET') {
    if (instance.state !== 'open') instance.state = 'connecting'
    return json({ base64: QR, code: '2@fake-qr', pairingCode: null, count: 1 })
  }
  if (area === 'instance' && action === 'logout' && method === 'DELETE') {
    instance.state = 'close'
    return json({ status: 'SUCCESS' })
  }
  if (area === 'webhook' && action === 'set' && method === 'POST') {
    instance.webhook = (await request.json()).webhook
    return json({ webhook: instance.webhook })
  }
  if (area === 'message' && action === 'sendText' && method === 'POST') {
    if (instance.state !== 'open') {
      return json(
        {
          status: 400,
          error: 'Bad Request',
          response: { message: ['not connected'] },
        },
        400,
      )
    }
    const body = await request.json()
    sent.push({ instance: name, number: body.number, text: body.text })
    return json({ key: { id: `sent-${sent.length}` }, status: 'PENDING' }, 201)
  }
  return json({ status: 404, error: 'Not Found' }, 404)
}

async function testApi(request, parts, url) {
  const [action, name] = parts
  if (action === 'instances' && request.method === 'GET') {
    return json(
      [...instances].map(([instanceName, { state }]) => ({
        instanceName,
        state,
      })),
    )
  }
  if (action === 'scan' && request.method === 'POST') {
    const instance = instances.get(name)
    if (!instance) return json({ error: 'unknown instance' }, 404)
    instance.state = 'open'
    return json({ instanceName: name, state: 'open' })
  }
  if (action === 'messages' && request.method === 'GET') {
    const number = url.searchParams.get('number')
    return json(sent.filter((message) => !number || message.number === number))
  }
  if (action === 'receive' && request.method === 'POST') {
    // A customer writes to the lava-jato's number: deliver the webhook like Evolution.
    const { instance: name, number, text } = await request.json()
    const instance = instances.get(name)
    if (!instance?.webhook?.url)
      return json({ error: 'no webhook for instance' }, 404)
    received += 1
    const response = await fetch(instance.webhook.url, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        ...(instance.webhook.headers ?? {}),
      },
      body: JSON.stringify({
        event: 'messages.upsert',
        instance: name,
        data: {
          key: {
            remoteJid: `${number}@s.whatsapp.net`,
            fromMe: false,
            id: `in-${received}`,
          },
          pushName: 'Cliente',
          message: { conversation: text },
          messageType: 'conversation',
        },
      }),
    })
    return json({ webhookStatus: response.status })
  }
  return json({ error: 'Not Found' }, 404)
}

Bun.serve({
  port: PORT,
  async fetch(request) {
    const url = new URL(request.url)
    const parts = url.pathname
      .split('/')
      .filter(Boolean)
      .map(decodeURIComponent)
    if (parts[0] === 'test') return testApi(request, parts.slice(1), url)
    return evolution(request, parts)
  },
})
console.log(`fake Evolution API on :${PORT}`)
