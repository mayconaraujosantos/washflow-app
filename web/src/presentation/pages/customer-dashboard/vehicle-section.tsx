import { CSSProperties, SubmitEvent, useEffect, useState } from 'react'
import { CreateVehicle } from '@/domain/usecases/create-vehicle'
import { ListVehiclesByCustomer } from '@/domain/usecases/list-vehicles-by-customer'
import { VehicleModel } from '@/domain/models/vehicle-model'
import { UiIcon } from '@/presentation/components/ui-icon'
import { EmptyState, Notice } from '@/presentation/components/ui/blocks'
import {
  errorMessage,
  isValidPlate,
  normalizePlate,
} from '@/presentation/helpers/format'

type Props = Readonly<{
  vehicles: VehicleModel[]
  customerId: string
  createVehicle: CreateVehicle
  listVehiclesByCustomer: ListVehiclesByCustomer
  onCreated: (vehicle: VehicleModel) => void
}>

const VEHICLE_COLORS = [
  { name: 'Branco', swatch: '#f5f5f5' },
  { name: 'Prata', swatch: '#c0c4c8' },
  { name: 'Preto', swatch: '#111111' },
  { name: 'Cinza', swatch: '#6b7075' },
  { name: 'Vermelho', swatch: '#c62828' },
  { name: 'Azul', swatch: '#1e5bb8' },
] as const

const MODEL_LIST_ID = 'vehicle-model-suggestions'

/** "Marca Modelo" names from the FIPE table, loaded only when the form shows. */
function useModelSuggestions(): string[] {
  const [models, setModels] = useState<string[]>([])
  useEffect(() => {
    let active = true
    import('./vehicle-models.json')
      .then((module) => {
        if (active) setModels(module.default)
      })
      .catch(() => {
        // Suggestions are a convenience: the field stays free text without them.
      })
    return () => {
      active = false
    }
  }, [])
  return models
}

function describe(vehicle: VehicleModel): string {
  return [vehicle.model, vehicle.color].filter(Boolean).join(' · ')
}

export function VehicleSection({
  vehicles,
  customerId,
  createVehicle,
  listVehiclesByCustomer,
  onCreated,
}: Props) {
  const [plate, setPlate] = useState('')
  const [model, setModel] = useState('')
  const [color, setColor] = useState('')
  const [otherColor, setOtherColor] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [counterCars, setCounterCars] = useState<VehicleModel[]>([])
  const [claiming, setClaiming] = useState<string | null>(null)
  const models = useModelSuggestions()

  useEffect(() => {
    let active = true
    listVehiclesByCustomer
      .listUnclaimed({ customerId })
      .then((cars) => {
        if (active) setCounterCars(cars)
      })
      .catch(() => {
        // Only a shortcut: the customer can still type the plate.
      })
    return () => {
      active = false
    }
  }, [customerId, listVehiclesByCustomer])

  const plateComplete = plate.length === 7
  const plateInvalid = plateComplete && !isValidPlate(plate)

  const add = async (params: CreateVehicle.Params) => {
    const vehicle = await createVehicle.create(params)
    setCounterCars((current) =>
      current.filter((car) => car.plate !== vehicle.plate),
    )
    onCreated(vehicle)
  }

  const claim = async (car: VehicleModel) => {
    setError(null)
    setClaiming(car.id)
    try {
      await add({ customerId, plate: car.plate })
    } catch (cause) {
      setError(errorMessage(cause, 'Erro ao adicionar o veículo'))
    } finally {
      setClaiming(null)
    }
  }

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    if (!isValidPlate(plate)) {
      setError('Placa inválida. Use o formato ABC1234 ou ABC1D23.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      await add({
        customerId,
        plate,
        model: model.trim() || undefined,
        color: color.trim() || undefined,
      })
      setPlate('')
      setModel('')
      setColor('')
      setOtherColor(false)
    } catch (cause) {
      setError(errorMessage(cause, 'Erro ao cadastrar veículo'))
    } finally {
      setLoading(false)
    }
  }

  const pickColor = (value: string) => {
    setOtherColor(false)
    setColor((current) => (current === value ? '' : value))
  }

  return (
    <>
      <h2>Meus veículos</h2>
      {vehicles.length === 0 ? (
        <EmptyState icon="car" title="Nenhum veículo cadastrado ainda." />
      ) : (
        <ul className="list">
          {vehicles.map((vehicle) => (
            <li key={vehicle.id} className="client-chip">
              <span className="plate">{vehicle.plate}</span>
              <span className="muted">{describe(vehicle) || '—'}</span>
            </li>
          ))}
        </ul>
      )}

      {counterCars.length > 0 && (
        <section className="card form" aria-label="Carros do balcão">
          <h3>Já atendemos estes carros no seu telefone</h3>
          <p className="faint">
            Toque para adicionar à sua conta, sem digitar nada.
          </p>
          <ul className="list">
            {counterCars.map((car) => (
              <li key={car.id} className="client-chip">
                <span className="plate">{car.plate}</span>
                <span className="muted">{describe(car) || '—'}</span>
                <button
                  type="button"
                  className="btn btn-accent"
                  disabled={claiming !== null}
                  onClick={() => claim(car)}
                >
                  {claiming === car.id ? 'Adicionando...' : 'É meu'}
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}

      <form className="card form" onSubmit={handleSubmit}>
        <h3>Adicionar veículo</h3>
        <label className="field">
          <span>Placa</span>
          <input
            value={plate}
            onChange={(event) => setPlate(normalizePlate(event.target.value))}
            placeholder="ABC1D23"
            autoCapitalize="characters"
            autoComplete="off"
            spellCheck={false}
            maxLength={8}
            aria-invalid={plateInvalid}
            required
          />
          {plateInvalid && (
            <small className="field-error">
              Use o formato ABC1234 ou ABC1D23.
            </small>
          )}
        </label>
        <label className="field">
          <span>Modelo (opcional)</span>
          <input
            value={model}
            onChange={(event) => setModel(event.target.value)}
            list={MODEL_LIST_ID}
            placeholder="Comece a digitar: Onix, HB20, Strada..."
            autoComplete="off"
            maxLength={50}
          />
          <datalist id={MODEL_LIST_ID}>
            {models.map((name) => (
              <option key={name} value={name} />
            ))}
          </datalist>
        </label>
        <fieldset className="field">
          <legend>Cor (opcional)</legend>
          <div
            className="choice-grid color-grid"
            role="radiogroup"
            aria-label="Cor"
          >
            {VEHICLE_COLORS.map(({ name, swatch }) => (
              <button
                key={name}
                type="button"
                role="radio"
                aria-checked={!otherColor && color === name}
                className="choice"
                onClick={() => pickColor(name)}
              >
                <span
                  className="swatch"
                  style={{ '--swatch': swatch } as CSSProperties}
                  aria-hidden="true"
                />
                {name}
              </button>
            ))}
            <button
              type="button"
              role="radio"
              aria-checked={otherColor}
              className="choice"
              onClick={() => {
                setOtherColor(true)
                setColor('')
              }}
            >
              Outra
            </button>
          </div>
          {otherColor && (
            <input
              value={color}
              onChange={(event) => setColor(event.target.value)}
              aria-label="Qual cor?"
              placeholder="Qual cor?"
              maxLength={30}
              autoFocus
            />
          )}
        </fieldset>
        <Notice tone="error">{error}</Notice>
        <button type="submit" className="btn btn-accent" disabled={loading}>
          <UiIcon name="plus" size={18} />
          {loading ? 'Salvando...' : 'Adicionar veículo'}
        </button>
      </form>
    </>
  )
}
