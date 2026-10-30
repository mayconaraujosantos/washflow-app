import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Features } from '@/domain/usecases/features'
import { ChecklistModal } from './checklist-modal'

const makeMockFeatures = (): Features => ({
  saveChecklist: vi.fn(),
  getChecklists: vi.fn(),
  savePhoto: vi.fn(),
  getPhotos: vi.fn(),
  deletePhoto: vi.fn(),
  createTransaction: vi.fn(),
  listTransactions: vi.fn(),
  updateTransaction: vi.fn(),
  deleteTransaction: vi.fn(),
  transactionReport: vi.fn(),
  createCategory: vi.fn(),
  listCategories: vi.fn(),
})

const renderChecklistModal = (
  features: Features,
  type: 'ENTRY' | 'EXIT' = 'ENTRY',
) =>
  render(
    <ChecklistModal
      features={features}
      serviceOrderId="order-1"
      type={type}
      onClose={vi.fn()}
    />,
  )

describe('ChecklistModal', () => {
  it('renders the entry checklist heading', () => {
    renderChecklistModal(makeMockFeatures(), 'ENTRY')

    expect(
      screen.getByRole('heading', { name: 'Checklist de Entrada' }),
    ).toBeInTheDocument()
  })

  it('renders the exit checklist heading', () => {
    renderChecklistModal(makeMockFeatures(), 'EXIT')

    expect(
      screen.getByRole('heading', { name: 'Checklist de Saída' }),
    ).toBeInTheDocument()
  })

  it('renders entry checklist items', () => {
    renderChecklistModal(makeMockFeatures(), 'ENTRY')

    expect(screen.getByText('Quilometragem')).toBeInTheDocument()
    expect(screen.getByText('Nível do combustível')).toBeInTheDocument()
    expect(screen.getByText('Rochas no para-choque')).toBeInTheDocument()
  })

  it('renders exit checklist items', () => {
    renderChecklistModal(makeMockFeatures(), 'EXIT')

    expect(screen.getByText('Carro limpo por fora')).toBeInTheDocument()
    expect(screen.getByText('Carro limpo por dentro')).toBeInTheDocument()
    expect(screen.getByText('Pneus limpos')).toBeInTheDocument()
  })

  it('renders the signature canvas and clear button', () => {
    renderChecklistModal(makeMockFeatures())

    expect(screen.getByText('Assinatura:')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Limpar assinatura' }),
    ).toBeInTheDocument()
  })

  it('renders cancel and save buttons', () => {
    renderChecklistModal(makeMockFeatures())

    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Salvar checklist' }),
    ).toBeInTheDocument()
  })

  it('calls onClose when cancel button is clicked', () => {
    const onClose = vi.fn()
    render(
      <ChecklistModal
        features={makeMockFeatures()}
        serviceOrderId="order-1"
        type="ENTRY"
        onClose={onClose}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onClose).toHaveBeenCalled()
  })

  it('toggles a checklist item when checkbox is clicked', () => {
    renderChecklistModal(makeMockFeatures())

    const checkbox = screen.getByLabelText('Quilometragem')
    expect(checkbox).not.toBeChecked()

    fireEvent.click(checkbox)
    expect(checkbox).toBeChecked()

    // Note input should appear when checked
    expect(screen.getByPlaceholderText('Observação...')).toBeInTheDocument()
  })

  it('saves checklist and calls onClose on submit', async () => {
    const features = makeMockFeatures()
    const savedChecklist = {
      id: 'cl-1',
      serviceOrderId: 'order-1',
      type: 'ENTRY',
      items: [{ label: 'Quilometragem', checked: false, note: null }],
      signatureImage: 'data:image/png;base64,xyz',
      createdAt: '2026-10-29T12:00:00Z',
      createdBy: 'user-1',
    }
    features.saveChecklist = vi.fn().mockResolvedValue(savedChecklist)
    const onClose = vi.fn()

    render(
      <ChecklistModal
        features={features}
        serviceOrderId="order-1"
        type="ENTRY"
        onClose={onClose}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Salvar checklist' }))

    await waitFor(() => {
      expect(features.saveChecklist).toHaveBeenCalledWith(
        'order-1',
        'ENTRY',
        expect.arrayContaining([expect.any(Object)]),
        null,
      )
    })

    await waitFor(() => {
      expect(onClose).toHaveBeenCalled()
    })
  })
})
