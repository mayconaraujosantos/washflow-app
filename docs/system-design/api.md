# API

A especificação completa (gerada do código) fica em **`/openapi`**, e a interface navegável em **`/swagger`**. Esta página resume os endpoints por área.

Convenções:
- **Perfil:** `—` = público; `A` = administrador; `G` = gerente; `L` = lavador; `C` = cliente.
- **Lava-jato:**
  - `conta`: vem do token.
  - `X-Est`: exige o cabeçalho `X-Establishment: <slug>` quando não há token.
  - `não`: a rota não usa lava-jato.
- Todo `POST` autenticado aceita `Idempotency-Key` (ver [confiabilidade.md](confiabilidade.md)).
- Datas de filtro (`from`, `to`, `date`) são dias no fuso `America/Manaus`, no formato `YYYY-MM-DD`.

## Plataforma e instalação

| Método | Rota | Perfil | Lava-jato | Descrição |
|---|---|---|---|---|
| GET | `/api/setup` | — | não | A plataforma já tem administrador? |
| POST | `/api/setup` | — | não | Cria o administrador (uma vez; `SETUP_CODE` em produção) |
| POST | `/api/auth/admins` | — | não | Login do administrador |
| GET | `/api/admin/establishments` | A | não | Lista os lava-jatos |
| POST | `/api/admin/establishments` | A | não | Cria lava-jato + gerente inicial + catálogo padrão |
| PATCH | `/api/admin/establishments/{id}/active` | A | não | Suspende ou reativa |
| GET | `/api/establishments?query=` | — | não | Lava-jatos ativos por nome (seletor do cliente) |
| GET | `/api/establishment` | — / G | X-Est / conta | Nome, horário, domingos e capacidade |
| PUT | `/api/establishment` | G | conta | Altera esses parâmetros |

## Autenticação

| Método | Rota | Perfil | Lava-jato | Descrição |
|---|---|---|---|---|
| POST | `/api/auth/customers` | — | X-Est | Cadastro (com `name`) ou login do cliente (telefone + senha) |
| POST | `/api/auth/customers/otp` | — | X-Est | Envia o código por WhatsApp |
| POST | `/api/auth/customers/otp/verify` | — | X-Est | Entra (ou cria a conta) com o código |
| POST | `/api/auth/customers/google` | — | X-Est | Entra com Google |
| GET | `/api/auth/options` | — | não | Logins sem senha habilitados |
| POST | `/api/auth/managers` | — | pela credencial | Gerente: `email` + senha (contas antigas: `phone`) |
| POST | `/api/auth/washers` | — | pela credencial | Lavador: telefone + senha |
| POST | `/api/auth/washers/pin` | — | pela credencial | Lavador: telefone + PIN (bloqueia após 5 erros) |
| POST | `/api/auth/washers/badge` | — | pela credencial | Lavador: token do QR do crachá |

## Operação do pátio

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | `/api/operations/entries` | G | Entrada pela placa (sem `scheduledAt` → `AGUARDANDO_PATIO`; com → `AGENDADO`). 409 se a placa já está no pátio ou a hora lotou |
| GET | `/api/operations/vehicles?query=` | G | Busca por início da placa, nome ou telefone (mín. 2 caracteres, até 10) |
| GET | `/api/operations/yard` | G | Pátio (não finalizados, por prioridade) |
| GET | `/api/operations/agenda?from=&to=` | G | Agenda por data de agendamento (máx. 62 dias) |
| GET | `/api/operations/queue` | L | Fila `AGUARDANDO_PATIO` por `atualizado_em ASC` |
| GET | `/api/operations/my-orders` | L | Minhas lavagens `EM_LAVAGEM`/`PAUSADA` |
| GET | `/api/operations/report?from=&to=` | G | Faturamento, comissões, serviços, pagamentos, pausas e tempo médio (máx. 366 dias) |
| GET | `/api/schedule/availability?date=` | — / C / G | Horas do dia com `booked`/`capacity` (X-Est sem token) |

## Atendimentos

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | `/api/service-orders` | C | Agenda para um veículo próprio (horário, antecedência de 30 min, capacidade) |
| GET | `/api/service-orders/{id}` | C (próprio) | Consulta o atendimento |
| GET | `/api/customers/{id}/service-orders` | C (próprio) | Atendimentos do cliente (painel, polling de 3 s) |
| PATCH | `/api/service-orders/{id}/check-in` | G | `AGENDADO` → `AGUARDANDO_PATIO` |
| PATCH | `/api/service-orders/{id}/start` | L | `AGUARDANDO_PATIO` → `EM_LAVAGEM` (409 se outro lavador pegou antes) |
| PATCH | `/api/service-orders/{id}/pause` | L (dono) | `EM_LAVAGEM` → `PAUSADA` (`reason`: `LUNCH`, `RESTROOM`, `OTHER` + `description`) |
| PATCH | `/api/service-orders/{id}/resume` | L (dono) | `PAUSADA` → `EM_LAVAGEM` |
| PATCH | `/api/service-orders/{id}/complete` | L | `EM_LAVAGEM` → `PRONTO` |
| PATCH | `/api/service-orders/{id}/finalize` | G | `PRONTO` → `FINALIZADO` + pagamento (`paymentMethod`) + comissão, em uma transação |
| POST | `/api/service-orders/{id}/ticket` | G | Token público do atendimento (sempre o mesmo) |
| GET | `/api/tickets/{token}` | — | Ticket público: veículo, serviço e status |
| POST/GET | `/api/service-orders/{id}/checklist` | G | Checklist de entrada ou saída com assinatura (regravar substitui) |
| POST/GET | `/api/service-orders/{id}/photos` | G | Fotos de entrada ou saída |
| DELETE | `/api/service-orders/{id}/photos/{photoId}` | G | Remove uma foto |

## Clientes e veículos

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | `/api/vehicles` | C | Cadastra veículo na própria conta (placa única no lava-jato) |
| GET | `/api/customers/{id}/vehicles` | C (próprio) | Veículos do cliente |

## Catálogo, vendas e caixa

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/services` | — (X-Est) / todos | Catálogo de serviços e produtos |
| POST | `/api/services` | G | Novo item (`name`, `price`, `washerCommission`) |
| POST | `/api/sales` | G | Venda rápida (`QUICK_SALE`) ou orçamento (`BUDGET`); preços do catálogo; devolve `paidAmount`/`outstandingAmount` |
| GET | `/api/sales?from=&to=[&type=]` | G | Vendas e orçamentos por dia de criação |
| POST/GET | `/api/cash-transactions` | G | Lançamentos de caixa (`INCOME`/`EXPENSE`), filtros por período |
| PUT/DELETE | `/api/cash-transactions/{id}` | G | Altera ou exclui |
| GET | `/api/cash-transactions/report` | G | Fluxo de caixa por categoria |
| POST/GET | `/api/expense-categories` | G | Categorias de despesa |

## Equipe e contas

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/users` | G | Contas do lava-jato |
| POST | `/api/users` | G | Cria lavador (`name`, `phone`, senha opcional ≥ 8) |
| PATCH | `/api/users/{id}/profile` | G | `CUSTOMER`, `WASHER` ou `MANAGER` (bloqueado com lavagens ativas) |
| PATCH | `/api/users/{id}/active` | G | Ativa ou desativa (bloqueado com lavagens ativas) |
| PATCH | `/api/users/{id}/email` | G | E-mail de acesso |
| POST | `/api/users/{id}/badge` | G | Novo crachá (PIN + QR, mostrados uma vez) |

## Sistema e observabilidade

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/health` | — | Saúde e conexão com o banco |
| GET | `/api/metrics` | Bearer `METRICS_TOKEN` | Prometheus (404 sem a variável) |
| POST | `/api/client-errors` | — | Erro do navegador (20/min por IP) |
| GET | `/openapi`, `/swagger` | — | Especificação e UI da API |

## Cabeçalhos

| Cabeçalho | Direção | Uso |
|---|---|---|
| `Authorization: Bearer <jwt>` | requisição | Rotas autenticadas |
| `X-Establishment: <slug>` | requisição | Rotas públicas do cliente sem token |
| `Idempotency-Key: <chave>` | requisição | POST autenticado seguro para reenvio |
| `X-Request-Id` | ambos | Rastreio; devolvido em toda resposta |
| `Idempotent-Replayed: true` | resposta | Resposta reproduzida de uma chave já usada |
