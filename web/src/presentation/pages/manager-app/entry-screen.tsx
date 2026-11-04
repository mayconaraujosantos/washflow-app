import { SubmitEvent, useCallback, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import type { VehicleLookupModel } from '@/domain/models/vehicle-lookup-model'
import { toManausInstant } from '@/main/config/business-time'
import { UiIcon } from '@/presentation/components/ui-icon'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import { Notice } from '@/presentation/components/ui/blocks'
import {
  ServicePicker,
  SlotPicker,
} from '@/presentation/components/ui/schedule-pickers'
import {
  errorMessage,
  formatDateTime,
  normalizePlate,
} from '@/presentation/helpers/format'
import { useManager } from './manager-context'
import { VehicleSearch } from './vehicle-search'

type Props = Readonly<{ mode: 'entry' | 'schedule' }>

/**
 * "Registrar entrada" (car is at the counter now -> straight to the yard) and
 * "Agendar" (manager books it for a customer). One guided form: who (search
 * or a new plate), which service (cards), when (free slots only) and optional
 * extras; the button summarises what will be saved.
 */
export function EntryScreen({ mode }: Props) {
  const {
    operations,
    servicePrices,
    notify,
    refresh,
    settings,
    establishment,
  } = useManager()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [selected, setSelected] = useState<VehicleLookupModel | null>(null)
  const [newClient, setNewClient] = useState(false)
  const [plate, setPlate] = useState('')
  const [vehicleModel, setVehicleModel] = useState('')
  const [vehicleColor, setVehicleColor] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [servicePriceId, setServicePriceId] = useState<number | null>(null)
  const [slot, setSlot] = useState<string | null>(null)
  const [moreOpen, setMoreOpen] = useState(false)
  const [expectedDelivery, setExpectedDelivery] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const isSchedule = mode === 'schedule'
  const vehiclePlate = selected?.plate ?? (newClient ? plate : '')
  const hasVehicle = vehiclePlate.length === 7
  const service = servicePrices.find((item) => item.id === servicePriceId)
  const ready = hasVehicle && service && (!isSchedule || slot)
  const loadDay = useCallback(
    (date: string) => establishment.availability(date),
    [establishment],
  )

  const startNewClient = (typedPlate = '') => {
    setSelected(null)
    setNewClient(true)
    setPlate(normalizePlate(typedPlate))
  }

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    if (!hasVehicle) {
      setError('Busque o cliente ou cadastre a placa do veículo.')
      return
    }
    if (!service) {
      setError('Escolha o serviço.')
      return
    }
    if (isSchedule && !slot) {
      setError('Escolha o dia e um horário livre.')
      return
    }
    setSaving(true)
    try {
      const order = await operations.registerEntry({
        plate: vehiclePlate,
        servicePriceId: service.id,
        vehicleModel: selected ? undefined : vehicleModel || undefined,
        vehicleColor: selected ? undefined : vehicleColor || undefined,
        customerName: selected ? undefined : customerName || undefined,
        customerPhone: selected ? undefined : customerPhone || undefined,
        scheduledAt: isSchedule && slot ? slot : undefined,
        expectedDeliveryAt: expectedDelivery
          ? toManausInstant(expectedDelivery)
          : undefined,
        notes: notes || undefined,
      })
      notify(
        'success',
        isSchedule
          ? `Agendamento de ${vehiclePlate} criado.`
          : `Entrada de ${vehiclePlate} registrada. Já está na fila do pátio.`,
      )
      await refresh()
      if (isSchedule) {
        navigate('/gerente/agenda')
      } else {
        navigate('/gerente/patio', { state: { ticketOrderId: order.id } })
      }
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível registrar'))
    } finally {
      setSaving(false)
    }
  }

  const summary = [
    vehiclePlate || null,
    service?.name ?? null,
    isSchedule && slot ? formatDateTime(slot) : null,
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <ScreenLayout
      title={isSchedule ? 'Agendar lavagem' : 'Registrar entrada'}
      backTo="/gerente"
    >
      <form className="form" onSubmit={handleSubmit}>
        <section className="step">
          <h2 className="step-title">
            <span className="step-number">1</span> Cliente e veículo
          </h2>
          {!newClient && (
            <VehicleSearch
              selected={selected}
              onSelect={(vehicle) => {
                setSelected(vehicle)
                if (vehicle) setNewClient(false)
              }}
              onCreate={startNewClient}
            />
          )}
          {!selected && !newClient && (
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => startNewClient()}
            >
              <UiIcon name="plus" size={18} />
              Cliente novo (cadastrar placa)
            </button>
          )}
          {newClient && (
            <div className="card form">
              <label className="field">
                <span>Placa *</span>
                <input
                  value={plate}
                  onChange={(event) =>
                    setPlate(normalizePlate(event.target.value))
                  }
                  placeholder="ABC1D23"
                  autoCapitalize="characters"
                  autoFocus
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
              <div className="field-row">
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
                  <span>WhatsApp</span>
                  <input
                    type="tel"
                    inputMode="tel"
                    value={customerPhone}
                    onChange={(event) => setCustomerPhone(event.target.value)}
                    placeholder="(92) 99999-0000"
                  />
                </label>
              </div>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => setNewClient(false)}
              >
                Buscar cliente já cadastrado
              </button>
            </div>
          )}
        </section>

        <section className="step">
          <h2 className="step-title">
            <span className="step-number">2</span> Serviço
          </h2>
          <ServicePicker
            services={servicePrices}
            value={servicePriceId}
            onChange={setServicePriceId}
          />
        </section>

        {isSchedule && (
          <section className="step">
            <h2 className="step-title">
              <span className="step-number">3</span> Dia e horário
            </h2>
            <SlotPicker
              loadDay={loadDay}
              openOnSunday={settings?.openOnSunday ?? false}
              initialDate={searchParams.get('data')}
              value={slot}
              onChange={setSlot}
            />
          </section>
        )}

        <button
          type="button"
          className="btn btn-ghost"
          aria-expanded={moreOpen}
          onClick={() => setMoreOpen((open) => !open)}
        >
          Mais opções (previsão de entrega, observações)
          <UiIcon name="chevronDown" size={18} />
        </button>
        {moreOpen && (
          <>
            <label className="field">
              <span>Previsão de entrega</span>
              <input
                type="datetime-local"
                value={expectedDelivery}
                onChange={(event) => setExpectedDelivery(event.target.value)}
              />
            </label>
            <label className="field">
              <span>Observações</span>
              <textarea
                value={notes}
                maxLength={500}
                onChange={(event) => setNotes(event.target.value)}
                placeholder="Ex.: risco na porta traseira, cliente aguarda na loja"
              />
            </label>
          </>
        )}

        <Notice tone="error">{error}</Notice>

        <button
          type="submit"
          className="btn btn-accent btn-block submit-summary"
          disabled={saving || !ready}
        >
          <span>
            <UiIcon name={isSchedule ? 'calendar' : 'entry'} size={18} />
            {saving
              ? 'Salvando...'
              : isSchedule
                ? 'Confirmar agendamento'
                : 'Registrar entrada'}
          </span>
          {summary && <small>{summary}</small>}
        </button>
      </form>
    </ScreenLayout>
  )
}
