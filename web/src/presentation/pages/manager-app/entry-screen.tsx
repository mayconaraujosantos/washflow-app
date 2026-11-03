import { SubmitEvent, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import type { VehicleLookupModel } from '@/domain/models/vehicle-lookup-model'
import { toManausInstant } from '@/main/config/business-time'
import { UiIcon } from '@/presentation/components/ui-icon'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import { Notice } from '@/presentation/components/ui/blocks'
import {
  errorMessage,
  formatBusinessHours,
  formatMoney,
  normalizePlate,
} from '@/presentation/helpers/format'
import { useManager } from './manager-context'
import { VehicleSearch } from './vehicle-search'

type Props = Readonly<{ mode: 'entry' | 'schedule' }>

/**
 * "Registrar entrada" (car is at the counter now -> straight to the yard) and
 * "Agendar" (manager books it for a customer). Same form; the schedule mode
 * adds the date/time and follows the business-hours rules on the API.
 */
export function EntryScreen({ mode }: Props) {
  const { operations, servicePrices, notify, refresh, settings } = useManager()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const presetDate = searchParams.get('data')

  const [selected, setSelected] = useState<VehicleLookupModel | null>(null)
  const [newClient, setNewClient] = useState(false)
  const [plate, setPlate] = useState('')
  const [vehicleModel, setVehicleModel] = useState('')
  const [vehicleColor, setVehicleColor] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [servicePriceId, setServicePriceId] = useState('')
  const [scheduledAt, setScheduledAt] = useState(
    presetDate ? `${presetDate}T08:00` : '',
  )
  const [expectedDelivery, setExpectedDelivery] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const isSchedule = mode === 'schedule'
  const hasVehicle = selected !== null || (newClient && plate.length === 7)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    if (!hasVehicle) {
      setError('Busque um cliente ou cadastre um novo com a placa.')
      return
    }
    setSaving(true)
    try {
      const order = await operations.registerEntry({
        plate: selected?.plate ?? plate,
        servicePriceId: Number(servicePriceId),
        vehicleModel: selected ? undefined : vehicleModel || undefined,
        vehicleColor: selected ? undefined : vehicleColor || undefined,
        customerName: selected ? undefined : customerName || undefined,
        customerPhone: selected ? undefined : customerPhone || undefined,
        scheduledAt: isSchedule ? toManausInstant(scheduledAt) : undefined,
        expectedDeliveryAt: expectedDelivery
          ? toManausInstant(expectedDelivery)
          : undefined,
        notes: notes || undefined,
      })
      const shownPlate = order.details?.vehiclePlate ?? selected?.plate ?? plate
      notify(
        'success',
        isSchedule
          ? `Agendamento de ${shownPlate} criado.`
          : `Entrada de ${shownPlate} registrada. Já está na fila do pátio.`,
      )
      await refresh()
      navigate(isSchedule ? '/gerente/agenda' : '/gerente/patio')
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível registrar'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <ScreenLayout
      title={isSchedule ? 'Agendar lavagem' : 'Registrar entrada'}
      backTo="/gerente"
    >
      <form className="form" onSubmit={handleSubmit}>
        <VehicleSearch
          selected={selected}
          onSelect={(vehicle) => {
            setSelected(vehicle)
            if (vehicle) setNewClient(false)
          }}
        />

        {!selected && !newClient && (
          <div className="client-box">
            <UiIcon name="alert" size={40} />
            <p>Nenhum cliente selecionado</p>
            <button
              type="button"
              className="btn btn-brand"
              onClick={() => setNewClient(true)}
            >
              <UiIcon name="plus" size={18} />
              Novo cliente
            </button>
          </div>
        )}

        {!selected && newClient && (
          <fieldset className="card form">
            <legend className="field-label">
              Novo cliente · só a placa é obrigatória
            </legend>
            <label className="field">
              <span>Placa</span>
              <input
                value={plate}
                onChange={(event) =>
                  setPlate(normalizePlate(event.target.value))
                }
                placeholder="ABC1D23"
                autoCapitalize="characters"
                required
              />
            </label>
            <div className="field-row">
              <label className="field">
                <span>Modelo</span>
                <input
                  value={vehicleModel}
                  maxLength={50}
                  onChange={(event) => setVehicleModel(event.target.value)}
                  placeholder="Onix"
                />
              </label>
              <label className="field">
                <span>Cor</span>
                <input
                  value={vehicleColor}
                  maxLength={30}
                  onChange={(event) => setVehicleColor(event.target.value)}
                  placeholder="Prata"
                />
              </label>
            </div>
            <label className="field">
              <span>Nome do cliente</span>
              <input
                value={customerName}
                maxLength={100}
                onChange={(event) => setCustomerName(event.target.value)}
                autoComplete="off"
              />
            </label>
            <label className="field">
              <span>Telefone (WhatsApp)</span>
              <input
                type="tel"
                inputMode="tel"
                value={customerPhone}
                onChange={(event) => setCustomerPhone(event.target.value)}
                placeholder="(92) 99999-0000"
              />
              <span className="field-hint">
                Se o telefone já tiver conta de cliente, o veículo é vinculado a
                ela.
              </span>
            </label>
          </fieldset>
        )}

        <label className="field">
          <span>Serviço</span>
          <select
            value={servicePriceId}
            onChange={(event) => setServicePriceId(event.target.value)}
            required
          >
            <option value="" disabled>
              Selecione o serviço...
            </option>
            {servicePrices.map((price) => (
              <option key={price.id} value={price.id}>
                {price.name} · {formatMoney(price.price)}
              </option>
            ))}
          </select>
        </label>

        {isSchedule && (
          <label className="field">
            <span>
              Data e horário (Manaus · {formatBusinessHours(settings)})
            </span>
            <input
              type="datetime-local"
              value={scheduledAt}
              onChange={(event) => setScheduledAt(event.target.value)}
              required
            />
          </label>
        )}

        <label className="field">
          <span>Previsão de entrega (opcional)</span>
          <input
            type="datetime-local"
            value={expectedDelivery}
            onChange={(event) => setExpectedDelivery(event.target.value)}
          />
        </label>

        <label className="field">
          <span>Observações (opcional)</span>
          <textarea
            value={notes}
            maxLength={500}
            onChange={(event) => setNotes(event.target.value)}
            placeholder="Ex.: risco na porta traseira, cliente aguarda na loja"
          />
        </label>

        <Notice tone="error">{error}</Notice>

        <button
          type="submit"
          className="btn btn-accent btn-block"
          disabled={saving}
        >
          <UiIcon name={isSchedule ? 'calendar' : 'entry'} size={18} />
          {saving
            ? 'Salvando...'
            : isSchedule
              ? 'Confirmar agendamento'
              : 'Registrar entrada'}
        </button>
      </form>
    </ScreenLayout>
  )
}
