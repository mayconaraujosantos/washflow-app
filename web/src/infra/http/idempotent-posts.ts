import { HttpResponse } from '@/data/protocols/http/http-client'

/**
 * Protects authenticated POSTs (sale, cash entry, vehicle entry, booking...)
 * against double clicks and retries:
 *
 * - the same POST (same session, URL and body) already on its way is not sent
 *   again: the second call waits for and shares the first response;
 * - every POST carries an `Idempotency-Key`; when it fails (network drop,
 *   timeout, server error) the key is kept, so sending the same thing again
 *   reuses it and the server replays the original result if it had in fact
 *   succeeded. After a success the key is forgotten: an identical operation
 *   later is a new one.
 */
const inFlight = new Map<string, Promise<HttpResponse<unknown>>>()
const retryKeys = new Map<string, string>()

export function sendIdempotentPost<T>(
  fingerprint: string,
  send: (idempotencyKey: string) => Promise<HttpResponse<T>>,
): Promise<HttpResponse<T>> {
  const running = inFlight.get(fingerprint)
  if (running) return running as Promise<HttpResponse<T>>

  const key = retryKeys.get(fingerprint) ?? newKey()
  retryKeys.set(fingerprint, key)
  const request = send(key)
    .then((response) => {
      if (response.statusCode >= 200 && response.statusCode < 300) {
        retryKeys.delete(fingerprint)
      }
      return response
    })
    .finally(() => inFlight.delete(fingerprint))
  inFlight.set(fingerprint, request as Promise<HttpResponse<unknown>>)
  return request
}

/** Test helper: forget every pending key and request. */
export function resetIdempotentPosts(): void {
  inFlight.clear()
  retryKeys.clear()
}

function newKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`
}
