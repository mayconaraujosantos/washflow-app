import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Features } from '@/domain/usecases/features'
import { CashManager } from './cash-manager'

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

const renderCashManager = (features: Features) => render(<CashManager features={features} />)

describe('CashManager', () => {
  it('renders the section heading', () => {
    renderCashManager(makeMockFeatures())

    expect(
      screen.getByRole('heading', { name: '💰 Controle de Caixa' }),
    ).toBeInTheDocument()
  })

  it('loads transactions and categories on mount', async () => {
    const features = makeMockFeatures()
    features.listTransactions = vi.fn().mockResolvedValue([
      {
        id: 'tx-1',
        type: 'INCOME',
        amount: '100.00',
        referenceDate: '2026-10-29',
        categoryId: null,
        description: 'Test',
        serviceOrderId: null,
        createdAt: '2026-10-29T12:00:00Z',
        createdBy: 'user-1',
      },
    ])
    features.listCategories = vi.fn().mockResolvedValue([
      { id: 'cat-1', name: 'Supplies', color: '#FF0000', createdAt: '2026-10-29T12:00:00Z' },
    ])

    renderCashManager(features)

    await waitFor(() => {
      expect(features.listTransactions).toHaveBeenCalled()
      expect(features.listCategories).toHaveBeenCalled()
    })

    expect(screen.getByText('Test')).toBeInTheDocument()
  })

  it('shows empty state when no transactions exist', async () => {
    const features = makeMockFeatures()
    features.listTransactions = vi.fn().mockResolvedValue([])
    features.listCategories = vi.fn().mockResolvedValue([])

    renderCashManager(features)

    await waitFor(() => {
      expect(screen.getByText('Nenhuma transação encontrada')).toBeInTheDocument()
    })
  })

  it('creates a new expense transaction on form submit', async () => {
    const features = makeMockFeatures()
    features.listTransactions = vi.fn().mockResolvedValue([])
    features.listCategories = vi.fn().mockResolvedValue([])
    features.createTransaction = vi.fn().mockResolvedValue({
      id: 'tx-new',
      type: 'EXPENSE',
      amount: '50.00',
      referenceDate: '2026-10-29',
      categoryId: null,
      description: 'New expense',
      serviceOrderId: null,
      createdAt: '2026-10-29T12:00:00Z',
      createdBy: 'user-1',
    })

    renderCashManager(features)

    await waitFor(() => {
      expect(features.listTransactions).toHaveBeenCalled()
    })

    const amountInput = screen.getByPlaceholderText('0.00')
    const descriptionInput = screen.getByPlaceholderText('Descrição...')
    const submitButton = screen.getByRole('button', { name: 'Adicionar' })

    fireEvent.change(amountInput, { target: { value: '50.00' } })
    fireEvent.change(descriptionInput, { target: { value: 'New expense' } })
    fireEvent.click(submitButton)

    await waitFor(() => {
      expect(features.createTransaction).toHaveBeenCalledWith(
        'EXPENSE',
        '50.00',
        expect.any(String),
        null,
        'New expense',
        null,
      )
    })
  })

  it('deletes a transaction when delete button is clicked', async () => {
    const features = makeMockFeatures()
    features.listTransactions = vi.fn().mockResolvedValue([
      {
        id: 'tx-1',
        type: 'EXPENSE',
        amount: '30.00',
        referenceDate: '2026-10-29',
        categoryId: null,
        description: 'To delete',
        serviceOrderId: null,
        createdAt: '2026-10-29T12:00:00Z',
        createdBy: 'user-1',
      },
    ])
    features.listCategories = vi.fn().mockResolvedValue([])
    features.deleteTransaction = vi.fn().mockResolvedValue(undefined)

    renderCashManager(features)

    await waitFor(() => {
      expect(screen.getByText('To delete')).toBeInTheDocument()
    })

    const deleteButton = screen.getByRole('button', { name: '✕' })
    fireEvent.click(deleteButton)

    await waitFor(() => {
      expect(features.deleteTransaction).toHaveBeenCalledWith('tx-1')
    })

    expect(screen.queryByText('To delete')).not.toBeInTheDocument()
  })

  it('loads a report when generate button is clicked', async () => {
    const features = makeMockFeatures()
    features.listTransactions = vi.fn().mockResolvedValue([])
    features.listCategories = vi.fn().mockResolvedValue([])
    features.transactionReport = vi.fn().mockResolvedValue({
      from: '2026-10-01',
      to: '2026-10-31',
      totalIncome: '1000.00',
      totalExpense: '300.00',
      netBalance: '700.00',
      totalCount: 10,
      byCategory: [],
    })

    renderCashManager(features)

    await waitFor(() => {
      expect(features.listTransactions).toHaveBeenCalled()
    })

    const reportButton = screen.getByRole('button', { name: 'Gerar relatório' })
    fireEvent.click(reportButton)

    await waitFor(() => {
      expect(features.transactionReport).toHaveBeenCalled()
    })

    expect(screen.getByText('Entradas')).toBeInTheDocument()
    expect(screen.getByText('Saídas')).toBeInTheDocument()
    expect(screen.getByText('Saldo')).toBeInTheDocument()
  })
})
