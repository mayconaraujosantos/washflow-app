import { useEffect, useId, useState } from 'react'
import type { EstablishmentListingModel } from '@/domain/models/platform-model'
import type { EstablishmentDirectory } from '@/domain/usecases/platform'
import type { EstablishmentChoice } from '@/main/session/establishment-choice'
import { Notice } from '@/presentation/components/ui/blocks'

type Props = Readonly<{
  directory: EstablishmentDirectory
  onPick: (choice: EstablishmentChoice) => void
}>

const SEARCH_DELAY_MS = 250

/** "Qual é o seu lava-jato?": search of the active lava-jatos by name. */
export function EstablishmentPicker({ directory, onPick }: Props) {
  const inputId = useId()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<EstablishmentListingModel[] | null>(
    null,
  )
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const timer = window.setTimeout(() => {
      directory
        .search(query.trim())
        .then((found) => {
          if (!active) return
          setResults(found)
          setError(null)
        })
        .catch(() => {
          if (active) setError('Não foi possível buscar os lava-jatos.')
        })
    }, SEARCH_DELAY_MS)
    return () => {
      active = false
      window.clearTimeout(timer)
    }
  }, [directory, query])

  return (
    <section className="auth-card" aria-labelledby="picker-title">
      <h2 id="picker-title">Qual é o seu lava-jato?</h2>
      <p className="subtitle">
        Escolha onde você lava o carro para entrar ou criar sua conta.
      </p>
      <div className="field">
        <label htmlFor={inputId} className="visually-hidden">
          Buscar lava-jato
        </label>
        <input
          id={inputId}
          className="input"
          type="search"
          placeholder="Buscar pelo nome"
          value={query}
          maxLength={60}
          autoComplete="off"
          onChange={(event) => setQuery(event.target.value)}
        />
      </div>
      <Notice tone="error">{error}</Notice>
      {results && results.length === 0 && (
        <p className="muted" role="status">
          Nenhum lava-jato encontrado.
        </p>
      )}
      {results && results.length > 0 && (
        <ul className="picker-list" aria-label="Lava-jatos">
          {results.map((item) => (
            <li key={item.slug}>
              <button
                type="button"
                className="picker-option"
                onClick={() => onPick({ slug: item.slug, name: item.name })}
              >
                {item.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
