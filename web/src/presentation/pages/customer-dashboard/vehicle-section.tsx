import { SubmitEvent, useState } from 'react'
import { CreateVehicle } from '@/domain/usecases/create-vehicle'
import { VehicleModel } from '@/domain/models/vehicle-model'

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
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao cadastrar veículo')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="dashboard-section">
      <h2>Meus veículos</h2>

      {vehicles.length === 0 ? (
        <p className="empty-state">Nenhum veículo cadastrado ainda.</p>
      ) : (
        <ul className="vehicle-list">
          {vehicles.map((vehicle) => (
            <li key={vehicle.id}>
              <span className="vehicle-plate">{vehicle.plate}</span>
              <span className="vehicle-details">
                {[vehicle.model, vehicle.color].filter(Boolean).join(' · ') ||
                  '—'}
              </span>
            </li>
          ))}
        </ul>
      )}

      <form className="inline-form" onSubmit={handleSubmit}>
        <input
          type="text"
          value={plate}
          onChange={(event) => setPlate(event.target.value.toUpperCase())}
          placeholder="Placa (ABC1D23)"
          required
        />
        <input
          type="text"
          value={model}
          onChange={(event) => setModel(event.target.value)}
          placeholder="Modelo (opcional)"
        />
        <input
          type="text"
          value={color}
          onChange={(event) => setColor(event.target.value)}
          placeholder="Cor (opcional)"
        />
        <button type="submit" className="primary-btn" disabled={loading}>
          {loading ? 'Salvando...' : 'Adicionar veículo'}
        </button>
      </form>

      {error && <p className="error-message">{error}</p>}
    </section>
  )
}
