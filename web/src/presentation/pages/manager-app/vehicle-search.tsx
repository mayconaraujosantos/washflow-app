import { useEffect, useState } from 'react'
import type { VehicleLookupModel } from '@/domain/models/vehicle-lookup-model'
import { UiIcon } from '@/presentation/components/ui-icon'
import {
  errorMessage,
  formatPhone,
  normalizePlate,
} from '@/presentation/helpers/format'
import { useManager } from './manager-context'

const DEBOUNCE_MS = 300

type Props = Readonly<{
  selected: VehicleLookupModel | null
  onSelect: (vehicle: VehicleLookupModel | null) => void
  // Offered when the typed text is a plate nobody has yet.
  onCreate?: (plate: string) => void
}>

/** "Buscar cliente" box: plate prefix, owner name or phone, picked from a list. */
const PLATE = /^[A-Z]{3}\d[A-Z0-9]\d{2}$/

export function VehicleSearch({ selected, onSelect, onCreate }: Props) {
  const { operations } = useManager()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<VehicleLookupModel[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const trimmed = query.trim()
    if (trimmed.length < 2) return
    let active = true
    const timer = window.setTimeout(() => {
      operations
        .searchVehicles(trimmed)
        .then((found) => {
          if (!active) return
          setResults(found)
          setError(null)
        })
        .catch((cause) => {
          if (active) setError(errorMessage(cause, 'Falha na busca'))
        })
    }, DEBOUNCE_MS)
    return () => {
      active = false
      window.clearTimeout(timer)
    }
  }, [operations, query])

  if (selected) {
    return (
      <div className="client-chip">
        <div>
          <span className="plate">{selected.plate}</span>
          <p className="muted">
            {[selected.model, selected.color].filter(Boolean).join(' · ') ||
              'Veículo'}
          </p>
          {(selected.customerName || selected.customerPhone) && (
            <p className="faint">
              {[selected.customerName, formatPhone(selected.customerPhone)]
                .filter(Boolean)
                .join(' · ')}
            </p>
          )}
        </div>
        <button
          type="button"
          className="icon-btn"
          aria-label="Trocar cliente"
          onClick={() => onSelect(null)}
        >
          <UiIcon name="close" />
        </button>
      </div>
    )
  }

  const showResults = query.trim().length >= 2
  const normalized = normalizePlate(query)
  const typedPlate =
    PLATE.test(normalized) &&
    !results.some((vehicle) => vehicle.plate === normalized)
      ? normalized
      : null

  return (
    <div className="form">
      <label className="field">
        <span className="visually-hidden">Buscar cliente</span>
        <input
          className="input"
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Buscar cliente (placa, nome ou telefone)"
          autoComplete="off"
        />
      </label>
      {error && <p className="notice notice-error">{error}</p>}
      {showResults && results.length > 0 && (
        <div className="search-results">
          {results.map((vehicle) => (
            <button
              key={vehicle.vehicleId}
              type="button"
              className="picker-item"
              onClick={() => {
                onSelect(vehicle)
                setQuery('')
                setResults([])
              }}
            >
              <span>
                <span className="plate">{vehicle.plate}</span>{' '}
                <span className="muted">{vehicle.model}</span>
              </span>
              <span className="faint">{vehicle.customerName ?? ''}</span>
            </button>
          ))}
        </div>
      )}
      {showResults && results.length === 0 && !error && !typedPlate && (
        <p className="faint">
          Nenhum cliente encontrado para “{query.trim()}”.
        </p>
      )}
      {typedPlate && onCreate && (
        <button
          type="button"
          className="btn btn-brand"
          onClick={() => onCreate(typedPlate)}
        >
          <UiIcon name="plus" size={18} />
          Cadastrar placa {typedPlate}
        </button>
      )}
    </div>
  )
}
