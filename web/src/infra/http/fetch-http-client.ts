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
      body,
    }
  }
}
