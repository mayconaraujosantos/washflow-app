import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Features } from '@/domain/usecases/features'
import { PhotoGallery } from './photo-gallery'

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

const renderPhotoGallery = (features: Features) =>
  render(<PhotoGallery features={features} serviceOrderId="order-1" />)

describe('PhotoGallery', () => {
  it('renders the section heading', () => {
    renderPhotoGallery(makeMockFeatures())

    expect(
      screen.getByRole('heading', { name: 'Fotos do veículo' }),
    ).toBeInTheDocument()
  })

  it('renders the load photos button', () => {
    renderPhotoGallery(makeMockFeatures())

    expect(
      screen.getByRole('button', { name: 'Carregar fotos' }),
    ).toBeInTheDocument()
  })

  it('loads photos when load button is clicked', async () => {
    const features = makeMockFeatures()
    features.getPhotos = vi.fn().mockResolvedValue([
      {
        id: 'photo-1',
        serviceOrderId: 'order-1',
        type: 'ENTRY',
        imageData: 'data:image/png;base64,xyz',
        description: 'Front view',
        createdAt: '2026-10-29T12:00:00Z',
        createdBy: 'user-1',
      },
    ])

    renderPhotoGallery(features)

    fireEvent.click(screen.getByRole('button', { name: 'Carregar fotos' }))

    await waitFor(() => {
      expect(features.getPhotos).toHaveBeenCalledWith('order-1')
    })

    expect(screen.getByText('Front view')).toBeInTheDocument()
  })

  it('shows photo type label for EXIT photos', async () => {
    const features = makeMockFeatures()
    features.getPhotos = vi.fn().mockResolvedValue([
      {
        id: 'photo-2',
        serviceOrderId: 'order-1',
        type: 'EXIT',
        imageData: 'data:image/png;base64,abc',
        description: null,
        createdAt: '2026-10-29T12:00:00Z',
        createdBy: 'user-1',
      },
    ])

    renderPhotoGallery(features)

    fireEvent.click(screen.getByRole('button', { name: 'Carregar fotos' }))

    await waitFor(() => {
      expect(screen.getByText('Saída')).toBeInTheDocument()
    })
  })

  it('deletes a photo when delete button is clicked', async () => {
    const features = makeMockFeatures()
    features.getPhotos = vi.fn().mockResolvedValue([
      {
        id: 'photo-1',
        serviceOrderId: 'order-1',
        type: 'ENTRY',
        imageData: 'data:image/png;base64,xyz',
        description: 'To delete',
        createdAt: '2026-10-29T12:00:00Z',
        createdBy: 'user-1',
      },
    ])
    features.deletePhoto = vi.fn().mockResolvedValue(undefined)

    renderPhotoGallery(features)

    fireEvent.click(screen.getByRole('button', { name: 'Carregar fotos' }))

    await waitFor(() => {
      expect(screen.getByText('To delete')).toBeInTheDocument()
    })

    const deleteButton = screen.getByRole('button', { name: '✕' })
    fireEvent.click(deleteButton)

    await waitFor(() => {
      expect(features.deletePhoto).toHaveBeenCalledWith('photo-1')
    })

    expect(screen.queryByText('To delete')).not.toBeInTheDocument()
  })

  it('renders the upload form with type selector and file input', () => {
    renderPhotoGallery(makeMockFeatures())

    expect(screen.getByText('Tipo:')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Enviar foto' }),
    ).toBeInTheDocument()
  })

  it('renders the description input in the upload form', () => {
    renderPhotoGallery(makeMockFeatures())

    expect(screen.getByPlaceholderText('Descrição...')).toBeInTheDocument()
  })
})
