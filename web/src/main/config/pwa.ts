import { registerSW } from 'virtual:pwa-register'

const UPDATE_CHECK_MS = 60 * 60 * 1000

/**
 * Registers the service worker. In autoUpdate mode the page reloads itself as
 * soon as a newly deployed version takes control, so nobody keeps running the
 * previous release from the precache. Tabs left open all day (shop counter)
 * also look for a new version every hour.
 */
export function registerPwa(): void {
  if (!('serviceWorker' in navigator)) return
  registerSW({
    immediate: true,
    onRegisteredSW(_url, registration) {
      if (!registration) return
      window.setInterval(() => {
        void registration.update()
      }, UPDATE_CHECK_MS)
    },
  })
}
