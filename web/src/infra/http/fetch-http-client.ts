import {
  HttpClient,
  HttpRequest,
  HttpResponse,
} from '@/data/protocols/http/http-client'
import { loadAccessToken } from '@/main/session/access-token'

export class FetchHttpClient<T = unknown> implements HttpClient<T> {
  async request(data: HttpRequest): Promise<HttpResponse<T>> {
    const token = loadAccessToken()
    const response = await fetch(data.url, {
      method: data.method.toUpperCase(),
      headers: data.body
        ? {
            'Content-Type': 'application/json',
            ...data.headers,
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          }
        : {
            ...data.headers,
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
      body: data.body ? JSON.stringify(data.body) : undefined,
    })

    const body = await response.json().catch(() => undefined)

    return {
      statusCode: response.status,
      body: response.status >= 500 ? withSupportCode(body, response) : body,
    }
  }
}

/**
 * Server failures carry a request id (body or X-Request-Id header). Putting it
 * in the message lets the user quote it, and the server log line has the same id.
 */
const withSupportCode = (body: unknown, response: Response): unknown => {
  const payload =
    body && typeof body === 'object' ? (body as Record<string, unknown>) : {}
  const requestId =
    (typeof payload.requestId === 'string' && payload.requestId) ||
    response.headers?.get?.('X-Request-Id')
  if (!requestId) return body
  return {
    ...payload,
    error: `Erro no servidor. Tente novamente; se continuar, informe o código ${requestId}.`,
  }
}
