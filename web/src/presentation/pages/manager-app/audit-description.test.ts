import { describe, expect, it } from 'vitest'
import { describeAuditAction, auditDetails } from './audit-description'

describe('describeAuditAction', () => {
  it.each([
    ['DELETE', '/api/cash-transactions/{id}', 'Excluiu um lançamento do caixa'],
    ['PUT', '/api/cash-transactions/{id}', 'Alterou um lançamento do caixa'],
    ['POST', '/api/services', 'Cadastrou um produto ou serviço'],
    [
      'DELETE',
      '/api/service-orders/{id}/photos/{photoId}',
      'Apagou uma foto do atendimento',
    ],
    ['PATCH', '/api/users/{id}/profile', 'Alterou o perfil de uma conta'],
    [
      'PATCH',
      '/api/service-orders/{id}/finalize',
      'Finalizou um atendimento e registrou o pagamento',
    ],
  ])('%s %s', (method, route, text) => {
    expect(describeAuditAction(method, route)).toBe(text)
  })

  it('falls back to the raw route for anything unknown', () => {
    expect(describeAuditAction('POST', '/api/new-thing')).toBe(
      'POST /api/new-thing',
    )
  })
})

describe('auditDetails', () => {
  it('lists the sent fields as "campo: valor"', () => {
    expect(
      auditDetails('{"amount":49.9,"description":"Troco","password":"***"}'),
    ).toBe('amount: 49.9 · description: Troco · password: ***')
  })

  it('returns nothing for an empty or unreadable body', () => {
    expect(auditDetails('')).toBe('')
    expect(auditDetails('{"name":"cortado...')).toBe('{"name":"cortado...')
  })
})
