import { HttpClient, HttpRequest, HttpResponse } from '@/data/protocols/http/http-client'

export class FetchHttpClient<T = unknown> implements HttpClient<T> {
  async request(data: HttpRequest): Promise<HttpResponse<T>> {
    const response = await fetch(data.url, {
      method: data.method,
      headers: data.headers,
      body: data.body ? JSON.stringify(data.body) : undefined
    })

    const body = await response.json().catch(() => undefined)

    return {
      statusCode: response.status,
      body
    }
  }
}
