# Washflow - Especificação de Funcionalidades

## Visão Geral

Documento que lista todas as funcionalidades identificadas na análise comparativa com concorrentes (LavCar Fácil), priorizadas por impacto e complexidade.

---

## ✅ Funcionalidades Já Implementadas

| # | Funcionalidade | Status |
|---|---|---|
| 1 | Cadastro de usuários (Cliente, Lavador, Gerente) | ✓ Implementado |
| 2 | Cadastro de veículos com placa, modelo, cor | ✓ Implementado |
| 3 | Cadastro de serviços e preços | ✓ Implementado |
| 4 | Agendamento de ordens de serviço | ✓ Implementado |
| 5 | Fluxo completo de atendimento (Agendado → Check-in → Em lavagem → Pronto → Finalizado) | ✓ Implementado |
| 6 | Controle de comissões do lavador | ✓ Implementado |
| 7 | Pagamentos múltiplos (PIX, dinheiro, cartão) | ✓ Implementado |
| 8 | Pausas do lavador (almoço, banheiro, outro) | ✓ Implementado |
| 9 | Fila de espera | ✓ Implementado |
| 10 | Visão geral do pátio | ✓ Implementado |
| 11 | Relatórios operacionais (por período) | ✓ Implementado |
| 12 | Autenticação JWT | ✓ Implementado |
| 13 | PWA (Progressive Web App) | ✓ Implementado |
| 14 | Swagger UI para documentação da API | ✓ Implementado |

---

## 🆕 Funcionalidades a Implementar

### Prioridade ALTA

#### 1. 📋 Checklists de Entrada e Saída com Assinatura Digital ✓ IMPLEMENTADO
- **Status**: Implementado (29/10/2026)
- **Descrição**: Checklist inicial e final com assinatura digital para registrar o estado do veículo
- **Por que implementar**: Evita disputas sobre avarias pré-existentes e garante padrão de qualidade
- **Complexidade**: Média
- **Impacto**: Alto
- **Modelos necessários**:
  - `checklist_item`: Itens do checklist (ex: "Rochas no para-choque", "Arranhão na porta", "Quilometragem")
  - `service_order_checklist`: Checklist associado à ordem de serviço (entrada/saída) com assinatura digital
- **Campos principais**:
  - `id`, `service_order_id`, `type` (ENTRY/EXIT), `items` (JSON), `signature_image` (base64), `created_at`, `created_by`
- **API endpoints**:
  - `POST /api/service-orders/{id}/checklist/entry` - Criar checklist de entrada
  - `POST /api/service-orders/{id}/checklist/exit` - Criar checklist de saída
  - `GET /api/service-orders/{id}/checklist` - Buscar checklists da ordem
- **UI components**:
  - Canvas de assinatura digital
  - Formulário de checklist com itens pré-definidos e customizados
  - Visualização dos checklists na ordem de serviço

#### 2. 📸 Fotos nas Ordens de Serviço ✓ IMPLEMENTADO
- **Status**: Implementado (29/10/2026)
- **Descrição**: Registrar fotos do veículo na entrada e saída do serviço
- **Por que implementar**: Evidência visual do estado do carro, protege contra reclamações
- **Complexidade**: Média
- **Impacto**: Alto
- **Modelos necessários**:
  - `service_order_photo`: Fotos associadas à ordem de serviço
- **Campos principais**:
  - `id`, `service_order_id`, `type` (ENTRY/EXIT), `image_data` (base64 ou path), `description`, `created_at`, `created_by`
- **API endpoints**:
  - `POST /api/service-orders/{id}/photos` - Adicionar foto(s)
  - `GET /api/service-orders/{id}/photos` - Listar fotos da ordem
  - `DELETE /api/service-orders/{id}/photos/{photoId}` - Remover foto
- **UI components**:
  - Upload de fotos com preview
  - Galeria de fotos na ordem de serviço
  - Captura de câmera (mobile)
- **Armazenamento**: Fotos armazenadas como base64 no banco ou em pasta local com referência no banco

#### 3. 💰 Controle de Caixa e Despesas ✓ IMPLEMENTADO
- **Status**: Implementado (29/10/2026)
- **Descrição**: Registro de entradas e despesas separadas com relatórios de fluxo de caixa
- **Por que implementar**: Visibilidade financeira completa do negócio
- **Complexidade**: Média
- **Impacto**: Alto
- **Modelos necessários**:
  - `cash_transaction`: Transações de caixa (entrada/saída)
  - `expense_category`: Categorias de despesas (ex: "Produtos", "Manutenção", "Aluguel")
- **Campos principais**:
  - `cash_transaction`: `id`, `type` (INCOME/EXPENSE), `amount`, `category_id`, `description`, `reference_date`, `created_at`, `created_by`
  - `expense_category`: `id`, `name`, `color`, `created_at`
- **API endpoints**:
  - `POST /api/cash-transactions` - Criar transação
  - `GET /api/cash-transactions` - Listar transações (com filtros por período, tipo, categoria)
  - `PUT /api/cash-transactions/{id}` - Atualizar transação
  - `DELETE /api/cash-transactions/{id}` - Excluir transação
  - `GET /api/cash-transactions/report` - Relatório de fluxo de caixa
  - `POST /api/expense-categories` - Criar categoria
  - `GET /api/expense-categories` - Listar categorias
- **UI components**:
  - Formulário de transação
  - Lista de transações com filtros
  - Dashboard financeiro com gráficos
  - Gestão de categorias

---

### Prioridade MÉDIA

#### 4. 💬 Mensagens Automáticas WhatsApp
- **Descrição**: Envio automático de mensagens de confirmação, status do serviço e rechamada
- **Por que implementar**: Melhora a experiência do cliente e aumenta a fidelização
- **Complexidade**: Média
- **Impacto**: Alto
- **Integração**: API WhatsApp (Z-API, Twilio, ou similar)
- **Gatilhos**:
  - Ordem agendada → Confirmação
  - Status alterado → Atualização
  - Serviço finalizado → Rechamada

#### 5. 🏆 Programa de Fidelidade
- **Descrição**: Sistema de pontos ou descontos progressivos para clientes recorrentes
- **Por que implementar**: Incentiva o retorno do cliente e aumenta o lifetime value
- **Complexidade**: Média
- **Impacto**: Médio
- **Modelos**: `loyalty_program`, `loyalty_points`, `loyalty_reward`

#### 6. 📊 Relatórios Avançados
- **Descrição**: Relatórios de vendas, despesas, convênios, produtividade da equipe
- **Por que implementar**: Dashboards mais completos para tomada de decisão
- **Complexidade**: Média
- **Impacto**: Médio
- **Observação**: Washflow já tem relatório básico operacional

#### 7. 📅 Agendamento com Lembretes
- **Descrição**: Agenda com bloqueio de horários, fila de espera, confirmação automática e lembretes
- **Por que implementar**: Reduz no-shows e organiza melhor o fluxo de trabalho
- **Complexidade**: Alta
- **Impacto**: Médio
- **Observação**: Washflow já tem agendamento básico

---

### Prioridade BAIXA

#### 8. 📄 Orçamentos em PDF
- **Descrição**: Gerar orçamentos profissionais em PDF para enviar ao cliente
- **Por que implementar**: Profissionaliza o atendimento e facilita o fechamento de serviços
- **Complexidade**: Baixa
- **Impacto**: Médio

#### 9. ⭐ Pesquisa de Satisfação
- **Descrição**: Envio automático de avaliação após o serviço concluído
- **Por que implementar**: Feedback contínuo para melhorar o serviço
- **Complexidade**: Baixa
- **Impacto**: Médio

#### 10. 📦 Controle de Estoque
- **Descrição**: Controle de produtos (shampoos, ceras, etc.) com alertas de estoque baixo
- **Por que implementar**: Evita rupturas e desperdícios
- **Complexidade**: Média
- **Impacto**: Médio

#### 11. 🤝 Gestão de Convênios
- **Descrição**: Cadastro e controle de convênios com empresas ou seguradoras
- **Por que implementar**: Abre nova fonte de receita e parcerias
- **Complexidade**: Média
- **Impacto**: Baixo

#### 12. 📱 App Dedicado para o Cliente
- **Descrição**: Aplicativo onde o cliente acompanha o status em tempo real
- **Por que implementar**: Diferencial competitivo e melhora a experiência
- **Complexidade**: Alta
- **Impacto**: Alto
- **Observação**: Washflow já tem PWA, mas focado na operação

#### 13. 🅿️ Controle de Pátio Visual
- **Descrição**: Mapa visual das vagas do pátio com status ocupado/livre
- **Por que implementar**: Otimiza o espaço físico e evita confusão com veículos
- **Complexidade**: Média
- **Impacto**: Baixo
- **Observação**: Washflow já tem visão geral do pátio

#### 14. 🏷️ Branding Personalizado
- **Descrição**: Personalizar o app com logo, cores e nome do lava-jato
- **Por que implementar**: Profissionaliza e reforça a marca
- **Complexidade**: Baixa
- **Impacto**: Baixo

#### 15. 📍 Endereços dos Clientes
- **Descrição**: Cadastro de endereços para serviço de leva e traz
- **Por que implementar**: Permite oferecer serviço premium de busca/entrega
- **Complexidade**: Baixa
- **Impacto**: Baixo

---

## 🚀 Plano de Implementação - Top 3 Funcionalidades

### Fase 1: Checklists com Assinatura Digital
1. Criar migração do banco de dados
2. Implementar modelo e repository
3. Criar endpoints da API
4. Implementar UI de checklist e assinatura
5. Testes e validação

### Fase 2: Fotos nas Ordens de Serviço
1. Criar migração do banco de dados
2. Implementar modelo e repository
3. Criar endpoints da API
4. Implementar UI de upload e galeria
5. Testes e validação

### Fase 3: Controle de Caixa e Despesas
1. Criar migrações do banco de dados
2. Implementar modelos e repositories
3. Criar endpoints da API
4. Implementar UI de transações e dashboard
5. Testes e validação

---

## 📊 Métricas de Sucesso

- **Checklists**: 100% das ordens de serviço com checklist de entrada
- **Fotos**: Redução em 50% das disputas sobre avarias
- **Controle de Caixa**: Visibilidade completa das finanças diárias/semanais/mensais

---

## 📝 Notas

- Este documento deve ser atualizado conforme novas funcionalidades forem implementadas
- A prioridade pode ser ajustada com base no feedback dos usuários
- Funcionalidades de prioridade MÉDIA e BAIXA podem ser implementadas em fases futuras
