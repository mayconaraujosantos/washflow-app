import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ErrorBoundary } from './error-boundary'

const Broken = () => {
  throw new Error('render failed')
}

describe('ErrorBoundary', () => {
  it('shows a friendly screen and reports the crash', () => {
    const onError = vi.fn()
    vi.spyOn(console, 'error').mockImplementation(() => undefined)

    render(
      <ErrorBoundary onError={onError}>
        <Broken />
      </ErrorBoundary>,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Algo deu errado')
    expect(screen.getByRole('button', { name: 'Recarregar' })).toBeVisible()
    expect(onError).toHaveBeenCalledWith(
      expect.objectContaining({ message: 'render failed' }),
      expect.stringMatching(/^react/),
    )
  })

  it('renders children when nothing fails', () => {
    render(
      <ErrorBoundary onError={vi.fn()}>
        <p>tudo certo</p>
      </ErrorBoundary>,
    )

    expect(screen.getByText('tudo certo')).toBeInTheDocument()
  })
})
