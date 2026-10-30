import { beforeEach, describe, expect, it, vi } from 'vitest'
import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { RemoteFeatures } from './remote-features'

describe('RemoteFeatures', () => {
  const request = vi.fn()
  let features: RemoteFeatures

  beforeEach(() => {
    request.mockReset()
    request.mockResolvedValue({
      statusCode: HttpStatusCode.ok,
      body: {},
    })
    const httpClient: HttpClient = { request }
    features = new RemoteFeatures('https://washflow.test', httpClient)
  })

  // ==================== CHECKLISTS ====================

  describe('checklists', () => {
    it('saves a checklist with type, items and signature', async () => {
      const checklist = {
        id: 'cl-1',
        serviceOrderId: 'order-1',
        type: 'ENTRY',
        items: [{ label: 'Quilometragem', checked: true, note: null }],
        signatureImage: 'data:image/png;base64,abc',
        createdAt: '2026-10-29T12:00:00Z',
        createdBy: 'user-1',
      }
      request.mockResolvedValueOnce({
        statusCode: 201,
        body: checklist,
      })

      const result = await features.saveChecklist(
        'order-1',
        'ENTRY',
        [{ label: 'Quilometragem', checked: true, note: null }],
        'data:image/png;base64,abc',
      )

      expect(result).toEqual(checklist)
      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/service-orders/order-1/checklist',
        method: 'post',
        body: {
          type: 'ENTRY',
          items: [{ label: 'Quilometragem', checked: true, note: null }],
          signatureImage: 'data:image/png;base64,abc',
        },
      })
    })

    it('lists checklists for a service order', async () => {
      const checklists = [
        {
          id: 'cl-1',
          serviceOrderId: 'order-1',
          type: 'ENTRY',
          items: [],
          signatureImage: null,
          createdAt: '2026-10-29T12:00:00Z',
          createdBy: 'user-1',
        },
      ]
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.ok,
        body: checklists,
      })

      const result = await features.getChecklists('order-1')

      expect(result).toEqual(checklists)
      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/service-orders/order-1/checklist',
        method: 'get',
        body: undefined,
      })
    })
  })

  // ==================== PHOTOS ====================

  describe('photos', () => {
    it('saves a photo with type and image data', async () => {
      const photo = {
        id: 'photo-1',
        serviceOrderId: 'order-1',
        type: 'ENTRY',
        imageData: 'data:image/png;base64,xyz',
        description: 'Rocha no para-choque',
        createdAt: '2026-10-29T12:00:00Z',
        createdBy: 'user-1',
      }
      request.mockResolvedValueOnce({
        statusCode: 201,
        body: photo,
      })

      const result = await features.savePhoto(
        'order-1',
        'ENTRY',
        'data:image/png;base64,xyz',
        'Rocha no para-choque',
      )

      expect(result).toEqual(photo)
      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/service-orders/order-1/photos',
        method: 'post',
        body: {
          type: 'ENTRY',
          imageData: 'data:image/png;base64,xyz',
          description: 'Rocha no para-choque',
        },
      })
    })

    it('lists photos for a service order', async () => {
      const photos: Array<{
        id: string
        serviceOrderId: string
        type: 'ENTRY' | 'EXIT'
        imageData: string
        description: string | null
        createdAt: string
        createdBy: string | null
      }> = []
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.ok,
        body: photos,
      })

      await features.getPhotos('order-1')

      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/service-orders/order-1/photos',
        method: 'get',
        body: undefined,
      })
    })

    it('deletes a photo by id', async () => {
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.ok,
        body: undefined,
      })

      await features.deletePhoto('photo-1')

      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/service-orders/photos/photo-1',
        method: 'delete',
        body: undefined,
      })
    })
  })

  // ==================== CASH TRANSACTIONS ====================

  describe('cash transactions', () => {
    it('creates an income transaction', async () => {
      const transaction = {
        id: 'tx-1',
        type: 'INCOME',
        amount: '150.00',
        categoryId: 'cat-1',
        description: 'Payment',
        referenceDate: '2026-10-29',
        serviceOrderId: null,
        createdAt: '2026-10-29T12:00:00Z',
        createdBy: 'user-1',
      }
      request.mockResolvedValueOnce({
        statusCode: 201,
        body: transaction,
      })

      const result = await features.createTransaction(
        'INCOME',
        '150.00',
        '2026-10-29',
        'cat-1',
        'Payment',
        null,
      )

      expect(result).toEqual(transaction)
      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/cash-transactions',
        method: 'post',
        body: {
          type: 'INCOME',
          amount: '150.00',
          referenceDate: '2026-10-29',
          categoryId: 'cat-1',
          description: 'Payment',
          serviceOrderId: null,
        },
      })
    })

    it('lists transactions with date range', async () => {
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.ok,
        body: [],
      })

      await features.listTransactions('2026-10-01', '2026-10-31')

      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/cash-transactions?from=2026-10-01&to=2026-10-31',
        method: 'get',
        body: undefined,
      })
    })

    it('lists all transactions without date range', async () => {
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.ok,
        body: [],
      })

      await features.listTransactions(null, null)

      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/cash-transactions',
        method: 'get',
        body: undefined,
      })
    })

    it('updates a transaction', async () => {
      const updated = {
        id: 'tx-1',
        type: 'EXPENSE',
        amount: '200.00',
        categoryId: 'cat-1',
        description: 'Updated',
        referenceDate: '2026-10-29',
        serviceOrderId: null,
        createdAt: '2026-10-29T12:00:00Z',
        createdBy: 'user-1',
      }
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.ok,
        body: updated,
      })

      const result = await features.updateTransaction('tx-1', {
        amount: '200.00',
        description: 'Updated',
      })

      expect(result).toEqual(updated)
      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/cash-transactions/tx-1',
        method: 'put',
        body: { amount: '200.00', description: 'Updated' },
      })
    })

    it('deletes a transaction', async () => {
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.ok,
        body: undefined,
      })

      await features.deleteTransaction('tx-1')

      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/cash-transactions/tx-1',
        method: 'delete',
        body: undefined,
      })
    })

    it('loads a cash transaction report', async () => {
      const report = {
        from: '2026-10-01',
        to: '2026-10-31',
        totalIncome: '1000.00',
        totalExpense: '300.00',
        netBalance: '700.00',
        totalCount: 10,
        byCategory: [],
      }
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.ok,
        body: report,
      })

      const result = await features.transactionReport(
        '2026-10-01',
        '2026-10-31',
      )

      expect(result).toEqual(report)
      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/cash-transactions/report?from=2026-10-01&to=2026-10-31',
        method: 'get',
        body: undefined,
      })
    })
  })

  // ==================== EXPENSE CATEGORIES ====================

  describe('expense categories', () => {
    it('creates an expense category', async () => {
      const category = {
        id: 'cat-1',
        name: 'Supplies',
        color: '#FF0000',
        createdAt: '2026-10-29T12:00:00Z',
      }
      request.mockResolvedValueOnce({
        statusCode: 201,
        body: category,
      })

      const result = await features.createCategory('Supplies', '#FF0000')

      expect(result).toEqual(category)
      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/expense-categories',
        method: 'post',
        body: { name: 'Supplies', color: '#FF0000' },
      })
    })

    it('lists expense categories', async () => {
      const categories = [
        {
          id: 'cat-1',
          name: 'Supplies',
          color: '#FF0000',
          createdAt: '2026-10-29T12:00:00Z',
        },
      ]
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.ok,
        body: categories,
      })

      const result = await features.listCategories()

      expect(result).toEqual(categories)
      expect(request).toHaveBeenCalledWith({
        url: 'https://washflow.test/api/expense-categories',
        method: 'get',
        body: undefined,
      })
    })
  })

  // ==================== ERROR HANDLING ====================

  describe('error handling', () => {
    it('throws the server error message on failure', async () => {
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.badRequest,
        body: { error: 'Type must be INCOME or EXPENSE' },
      })

      await expect(
        features.createTransaction(
          'INVALID' as any,
          '100',
          '2026-10-29',
          null,
          null,
          null,
        ),
      ).rejects.toThrow('Type must be INCOME or EXPENSE')
    })

    it('throws a generic message when the server returns no error body', async () => {
      request.mockResolvedValueOnce({
        statusCode: HttpStatusCode.serverError,
        body: undefined,
      })

      await expect(features.listCategories()).rejects.toThrow(
        'Não foi possível concluir a operação',
      )
    })
  })
})
