import { SubmitEvent, useState } from 'react'
import { CreateVehicle } from '@/domain/usecases/create-vehicle'
import { VehicleModel } from '@/domain/models/vehicle-model'
import { UiIcon } from '@/presentation/components/ui-icon'
import { EmptyState, Notice } from '@/presentation/components/ui/blocks'
import { errorMessage, normalizePlate } from '@/presentation/helpers/format'

type Props = Readonly<{
  vehicles: VehicleModel[]
  customerId: string
  createVehicle: CreateVehicle
  onCreated: (vehicle: VehicleModel) => void
}>

export function VehicleSection({
  vehicles,
  customerId,
  createVehicle,
  onCreated,
}: Props) {
  const [plate, setPlate] = useState('')
  const [model, setModel] = useState('')
  const [color, setColor] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const vehicle = await createVehicle.create({
        customerId,
        plate,
        model: model || undefined,
        color: color || undefined,
      })
      onCreated(vehicle)
      setPlate('')
      setModel('')
      setColor('')
    } catch (cause) {
      setError(errorMessage(cause, 'Erro ao cadastrar veículo'))
    } finally {
      setLoading(false)
    }
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
              <span className="muted">
                {[vehicle.model, vehicle.color].filter(Boolean).join(' · ') ||
                  '—'}
              </span>
            </li>
          ))}
        </ul>
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
            required
          />
        </label>
        <div className="field-row">
          <label className="field">
            <span>Modelo (opcional)</span>
            <input
              value={model}
              onChange={(event) => setModel(event.target.value)}
            />
          </label>
          <label className="field">
            <span>Cor (opcional)</span>
            <input
              value={color}
              onChange={(event) => setColor(event.target.value)}
            />
          </label>
        </div>
        <Notice tone="error">{error}</Notice>
        <button type="submit" className="btn btn-accent" disabled={loading}>
          <UiIcon name="plus" size={18} />
          {loading ? 'Salvando...' : 'Adicionar veículo'}
        </button>
      </form>
    </>
  )
}
