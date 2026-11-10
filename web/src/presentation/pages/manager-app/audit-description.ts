// What each audited endpoint means to the manager. Keyed by "METHOD route",
// with the route pattern the API records (e.g. /api/cash-transactions/{id}).
const ACTIONS: Record<string, string> = {
  'POST /api/cash-transactions': 'Lançou uma entrada ou despesa no caixa',
  'PUT /api/cash-transactions/{id}': 'Alterou um lançamento do caixa',
  'DELETE /api/cash-transactions/{id}': 'Excluiu um lançamento do caixa',
  'POST /api/expense-categories': 'Criou uma categoria de despesa',
  'POST /api/services': 'Cadastrou um produto ou serviço',
  'POST /api/sales': 'Registrou uma venda ou orçamento',
  'POST /api/operations/entries': 'Registrou a entrada de um veículo',
  'PATCH /api/service-orders/{id}/check-in': 'Recebeu um veículo agendado',
  'PATCH /api/service-orders/{id}/finalize':
    'Finalizou um atendimento e registrou o pagamento',
  'POST /api/service-orders/{id}/photos': 'Adicionou uma foto ao atendimento',
  'DELETE /api/service-orders/{id}/photos/{photoId}':
    'Apagou uma foto do atendimento',
  'POST /api/service-orders/{id}/checklist':
    'Salvou o checklist do atendimento',
  'POST /api/service-orders/{id}/ticket': 'Gerou o ticket do cliente',
  'POST /api/users': 'Cadastrou um lavador',
  'POST /api/users/{id}/badge': 'Emitiu um crachá de lavador',
  'PATCH /api/users/{id}/profile': 'Alterou o perfil de uma conta',
  'PATCH /api/users/{id}/active': 'Ativou ou desativou uma conta',
  'PATCH /api/users/{id}/email': 'Alterou o e-mail de acesso de uma conta',
  'PUT /api/establishment': 'Alterou os dados do lava-jato',
}

export function describeAuditAction(method: string, route: string): string {
  return ACTIONS[`${method} ${route}`] ?? `${method} ${route}`
}

/** The recorded request body as "campo: valor · ..."; raw text when it isn't a JSON object. */
export function auditDetails(details: string): string {
  if (!details) return ''
  try {
    const body: unknown = JSON.parse(details)
    if (!body || typeof body !== 'object' || Array.isArray(body)) return details
    return Object.entries(body)
      .map(
        ([key, value]) =>
          `${key}: ${typeof value === 'object' ? JSON.stringify(value) : String(value)}`,
      )
      .join(' · ')
  } catch {
    // Cut at 2000 characters by the API: no longer valid JSON.
    return details
  }
}
