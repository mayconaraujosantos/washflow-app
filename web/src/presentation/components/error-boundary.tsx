import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = {
  children: ReactNode
  onError: (error: unknown, source: string) => void
}

type State = { failed: boolean }

/** Friendly screen instead of a blank page when a screen crashes. */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { failed: false }

  static getDerivedStateFromError(): State {
    return { failed: true }
  }

  componentDidCatch(error: unknown, info: ErrorInfo): void {
    const component = info.componentStack?.trim().split('\n')[0] ?? ''
    this.props.onError(error, `react ${component}`.trim())
  }

  render() {
    if (!this.state.failed) return this.props.children
    return (
      <main className="auth-shell" role="alert">
        <h1 className="auth-brand">Algo deu errado</h1>
        <p className="muted">
          A tela encontrou um erro inesperado e a equipe já foi avisada.
          Recarregue a página para continuar.
        </p>
        <button
          type="button"
          className="btn-primary"
          onClick={() => window.location.reload()}
        >
          Recarregar
        </button>
      </main>
    )
  }
}
