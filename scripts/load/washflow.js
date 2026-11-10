// Dia de pico do Washflow (k6), com as sessões criadas por seed_load.py.
//
// Cada tela aberta repete o que o PWA faz hoje:
//   lavador  - fila + minhas lavagens a cada 3 s (washer-app.tsx)
//   cliente  - seus atendimentos a cada 3 s (customer-dashboard.tsx)
//   gerente  - pátio a cada 3 s e indicadores do dia a cada 30 s (manager-app/manager-home)
//   ticket   - ticket público a cada 5 s (ticket-page.tsx)
// e carros passam pelo fluxo completo: entrada, início, conclusão e pagamento.
import http from 'k6/http'
import { check, sleep } from 'k6'
import exec from 'k6/execution'

const data = JSON.parse(open(__ENV.TOKENS || '/load/out/tokens.json'))
const BASE = __ENV.BASE_URL || 'http://localhost:7100'
const HOLD = __ENV.HOLD || '5m'
const FLOWS_PER_MIN = Number(__ENV.FLOWS_PER_MIN || 10)

const shops = data.shops
const washers = shops.flatMap((shop, s) => shop.washers.map((w) => ({ ...w, s })))
const customers = shops.flatMap((shop) => shop.customers)
const tickets = shops.flatMap((shop) => shop.tickets)

const auth = (token) => ({ Authorization: `Bearer ${token}` })
const json = (token) => ({ ...auth(token), 'Content-Type': 'application/json' })
const pick = (list) => list[(exec.vu.idInTest - 1) % list.length]

// Spread the first request over one refresh period, like screens opened at different times.
function stagger(seconds) {
  if (exec.vu.iterationInScenario === 0) sleep(Math.random() * seconds)
}

const ok = (response) => check(response, { 'status 2xx': (r) => r.status >= 200 && r.status < 300 })

export const options = {
  scenarios: {
    washers: { executor: 'constant-vus', vus: washers.length, duration: HOLD, exec: 'washer' },
    customers: { executor: 'constant-vus', vus: customers.length, duration: HOLD, exec: 'customer' },
    managers: { executor: 'constant-vus', vus: shops.length, duration: HOLD, exec: 'manager' },
    tickets: { executor: 'constant-vus', vus: tickets.length, duration: HOLD, exec: 'ticket' },
    flows: {
      executor: 'constant-arrival-rate',
      rate: FLOWS_PER_MIN,
      timeUnit: '1m',
      duration: HOLD,
      preAllocatedVUs: 10,
      maxVUs: 60,
      exec: 'flow',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    'http_req_duration{name:queue}': ['p(95)<800'],
    'http_req_duration{name:my-orders}': ['p(95)<800'],
    'http_req_duration{name:customer-orders}': ['p(95)<800'],
    'http_req_duration{name:yard}': ['p(95)<800'],
    'http_req_duration{name:report}': ['p(95)<2000'],
    'http_req_duration{name:ticket}': ['p(95)<800'],
    'http_req_duration{name:entry}': ['p(95)<1500'],
    'http_req_duration{name:finalize}': ['p(95)<1500'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
}

export function washer() {
  stagger(3)
  const me = pick(washers)
  ok(http.get(`${BASE}/api/operations/queue`, { headers: auth(me.token), tags: { name: 'queue' } }))
  ok(http.get(`${BASE}/api/operations/my-orders`, { headers: auth(me.token), tags: { name: 'my-orders' } }))
  sleep(3)
}

export function customer() {
  stagger(3)
  const me = pick(customers)
  ok(
    http.get(`${BASE}/api/customers/${me.id}/service-orders`, {
      headers: auth(me.token),
      tags: { name: 'customer-orders' },
    }),
  )
  sleep(3)
}

export function manager() {
  stagger(3)
  const shop = pick(shops)
  ok(http.get(`${BASE}/api/operations/yard`, { headers: auth(shop.managerToken), tags: { name: 'yard' } }))
  if (exec.vu.iterationInScenario % 10 === 0) {
    const today = new Date(Date.now() - 4 * 3600 * 1000).toISOString().slice(0, 10)
    ok(
      http.get(`${BASE}/api/operations/report?from=${today}&to=${today}`, {
        headers: auth(shop.managerToken),
        tags: { name: 'report' },
      }),
    )
  }
  sleep(3)
}

export function ticket() {
  stagger(5)
  ok(http.get(`${BASE}/api/tickets/${pick(tickets)}`, { tags: { name: 'ticket' } }))
  sleep(5)
}

export function flow() {
  const shop = shops[Math.floor(Math.random() * shops.length)]
  const washer = shop.washers[Math.floor(Math.random() * shop.washers.length)]
  // Brazilian plate (LLL0000), new on every flow so the car is never already in the yard.
  const letter = String.fromCharCode(65 + Math.floor(Math.random() * 26))
  const plate = `KZ${letter}${String(Math.floor(Math.random() * 1e4)).padStart(4, '0')}`
  const entry = http.post(
    `${BASE}/api/operations/entries`,
    JSON.stringify({ plate, servicePriceId: shop.serviceId }),
    { headers: json(shop.managerToken), tags: { name: 'entry' } },
  )
  if (!ok(entry)) return
  const id = entry.json('id')
  const steps = [
    ['start', washer.token, { washerId: washer.id }],
    ['complete', washer.token, {}],
    ['finalize', shop.managerToken, { paymentMethod: 'PIX' }],
  ]
  for (const [step, token, body] of steps) {
    const response = http.patch(`${BASE}/api/service-orders/${id}/${step}`, JSON.stringify(body), {
      headers: json(token),
      tags: { name: step },
    })
    if (!ok(response)) return
  }
}
