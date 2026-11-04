import { describe, expect, it, vi } from 'vitest'
import { ErrorReporter } from './error-reporter'

describe('ErrorReporter', () => {
  it('sends the message, stack, page and source', () => {
    const send = vi.fn()
    new ErrorReporter('/api/client-errors', send).report(
      new TypeError('x is undefined'),
      'react',
    )

    expect(send).toHaveBeenCalledWith(
      '/api/client-errors',
      expect.objectContaining({
        message: 'x is undefined',
        source: 'react',
        page: window.location.pathname,
        stack: expect.stringContaining('TypeError'),
      }),
    )
  })

  it('skips repeats and caps reports per page load', () => {
    const send = vi.fn()
    const reporter = new ErrorReporter('/e', send)

    reporter.report('same', 'a')
    reporter.report('same', 'a')
    for (let i = 0; i < 20; i++) reporter.report(`error ${i}`, 'a')

    expect(send).toHaveBeenCalledTimes(10)
  })

  it('never throws when sending fails', () => {
    const reporter = new ErrorReporter('/e', () => {
      throw new Error('offline')
    })

    expect(() => reporter.report('boom', 'a')).not.toThrow()
  })

  it('catches unhandled promise rejections once installed', () => {
    const send = vi.fn()
    const target = new EventTarget() as unknown as Window
    new ErrorReporter('/e', send).install(target)

    const event = new Event('unhandledrejection') as Event & { reason: unknown }
    event.reason = new Error('request failed')
    target.dispatchEvent(event)

    expect(send).toHaveBeenCalledWith(
      '/e',
      expect.objectContaining({
        message: 'request failed',
        source: 'unhandledrejection',
      }),
    )
  })
})
