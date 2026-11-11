import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { CreateVehicle } from '@/domain/usecases/create-vehicle'
import type { ListVehiclesByCustomer } from '@/domain/usecases/list-vehicles-by-customer'
import type { VehicleModel } from '@/domain/models/vehicle-model'
import { VehicleSection } from './vehicle-section'

const counterCar: VehicleModel = {
  id: 'vehicle-1',
  customerId: null as unknown as string,
  plate: 'QWE4R56',
  model: 'Onix',
  color: 'Prata',
}

function setup(unclaimed: VehicleModel[] = []) {
  const create = vi.fn(async (params: CreateVehicle.Params) => ({
    id: 'new-id',
    customerId: params.customerId,
    plate: params.plate,
    model: params.model ?? 'Onix',
    color: params.color ?? 'Prata',
  }))
  const listVehiclesByCustomer: ListVehiclesByCustomer = {
    list: vi.fn().mockResolvedValue([]),
    listUnclaimed: vi.fn().mockResolvedValue(unclaimed),
  }
  const onCreated = vi.fn()
  render(
    <VehicleSection
      vehicles={[]}
      customerId="customer-1"
      createVehicle={{ create }}
      listVehiclesByCustomer={listVehiclesByCustomer}
      onCreated={onCreated}
    />,
  )
  return { create, onCreated }
}

describe('VehicleSection', () => {
  it('registers a car with a typed plate, a suggested model and a color button', async () => {
    const { create, onCreated } = setup()

    await userEvent.type(screen.getByPlaceholderText('ABC1D23'), 'abc-1d23')
    expect(screen.getByPlaceholderText('ABC1D23')).toHaveValue('ABC1D23')
    await userEvent.type(screen.getByLabelText(/Modelo/), 'Hyundai HB20')
    await userEvent.click(screen.getByRole('radio', { name: 'Preto' }))
    expect(screen.getByRole('radio', { name: 'Preto' })).toHaveAttribute(
      'aria-checked',
      'true',
    )
    await userEvent.click(
      screen.getByRole('button', { name: /Adicionar veículo/ }),
    )

    expect(create).toHaveBeenCalledWith({
      customerId: 'customer-1',
      plate: 'ABC1D23',
      model: 'Hyundai HB20',
      color: 'Preto',
    })
    expect(onCreated).toHaveBeenCalled()
  })

  it('takes a typed color under "Outra"', async () => {
    const { create } = setup()

    await userEvent.type(screen.getByPlaceholderText('ABC1D23'), 'ABC1234')
    await userEvent.click(screen.getByRole('radio', { name: 'Outra' }))
    await userEvent.type(screen.getByLabelText('Qual cor?'), 'Verde')
    await userEvent.click(
      screen.getByRole('button', { name: /Adicionar veículo/ }),
    )

    expect(create).toHaveBeenCalledWith(
      expect.objectContaining({ plate: 'ABC1234', color: 'Verde' }),
    )
  })

  it('refuses a plate in the wrong format without calling the API', async () => {
    const { create } = setup()

    await userEvent.type(screen.getByPlaceholderText('ABC1D23'), '1234567')
    expect(screen.getByText('Use o formato ABC1234 ou ABC1D23.')).toBeVisible()
    await userEvent.click(
      screen.getByRole('button', { name: /Adicionar veículo/ }),
    )

    expect(create).not.toHaveBeenCalled()
  })

  it('adds a car the counter already registered with one tap', async () => {
    const { create, onCreated } = setup([counterCar])

    expect(await screen.findByText('QWE4R56')).toBeVisible()
    await userEvent.click(screen.getByRole('button', { name: 'É meu' }))

    expect(create).toHaveBeenCalledWith({
      customerId: 'customer-1',
      plate: 'QWE4R56',
    })
    expect(onCreated).toHaveBeenCalled()
    expect(screen.queryByText('QWE4R56')).not.toBeInTheDocument()
  })
})
