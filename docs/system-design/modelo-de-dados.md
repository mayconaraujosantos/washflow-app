# Modelo de dados

PostgreSQL. O esquema é criado pelas migrações em `src/main/resources/db/migration`, aplicadas pelo Flyway (`JdbiFactory.migrate`, histórico em `schema_history`). O dicionário campo a campo está em [CLAUDE.md](../../CLAUDE.md), seção 2.

## Diagrama entidade-relacionamento

```mermaid
erDiagram
    estabelecimentos ||--o{ usuarios : "equipe e clientes"
    estabelecimentos ||--o{ veiculos : ""
    estabelecimentos ||--o{ servicos_preco : "catálogo"
    estabelecimentos ||--o{ atendimentos : ""
    estabelecimentos ||--o{ vendas : ""
    estabelecimentos ||--o{ cash_transaction : ""
    estabelecimentos ||--o{ expense_category : ""
    estabelecimentos ||--o{ codigos_acesso : ""
    estabelecimentos |o--o{ chaves_idempotencia : ""

    usuarios |o--o{ veiculos : "cliente_id (anulável)"
    veiculos ||--o{ atendimentos : ""
    servicos_preco ||--o{ atendimento_servicos : ""
    atendimentos ||--|{ atendimento_servicos : "serviços selecionados"
    usuarios |o--o{ atendimentos : "lavador_id"
    atendimentos ||--o| pagamentos : "1 por atendimento"
    atendimentos ||--o{ comissoes_lavador : ""
    atendimentos ||--o{ pausas_atendimento : ""
    atendimentos ||--o{ service_order_checklist : "ENTRY / EXIT"
    atendimentos ||--o{ service_order_photo : ""
    vendas ||--|{ venda_itens : ""
    vendas ||--o{ venda_pagamentos : ""
    servicos_preco ||--o{ venda_itens : "preço congelado"
    veiculos |o--o{ vendas : "opcional"
    expense_category |o--o{ cash_transaction : ""

    estabelecimentos {
        uuid id PK
        varchar slug UK "link /?lavajato="
        varchar nome
        int hora_abertura
        int hora_fechamento "exclusiva"
        bool abre_domingo
        int max_veiculos_hora
        bool ativo
    }
    administradores {
        uuid id PK
        varchar email UK
        varchar senha_hash
    }
    usuarios {
        uuid id PK
        uuid estabelecimento_id FK
        varchar telefone "único no lava-jato"
        varchar email UK "login do gerente"
        varchar perfil "CLIENTE|LAVADOR|GERENTE"
        varchar senha_hash "BCrypt"
        varchar pin_hash "crachá"
        varchar cracha_hash "HMAC do QR"
        varchar google_sub
        bool ativo
    }
    veiculos {
        uuid id PK
        uuid estabelecimento_id FK
        uuid cliente_id FK
        varchar placa "única no lava-jato"
        varchar contato_nome
        varchar contato_telefone
    }
    servicos_preco {
        int id PK
        uuid estabelecimento_id FK
        varchar nome
        decimal preco
        decimal comissao_lavador
    }
    atendimentos {
        uuid id PK
        uuid estabelecimento_id FK
        uuid veiculo_id FK
        int servico_preco_id FK "primeiro serviço; compatibilidade"
        uuid lavador_id FK
        varchar status
        timestamptz data_agendamento
        timestamptz atualizado_em "ordena a fila"
        timestamptz lavagem_iniciada_em
        timestamptz lavagem_concluida_em
        varchar ticket_token "link público"
    }
    pagamentos {
        uuid id PK
        uuid atendimento_id FK, UK
        varchar forma_pagamento
        decimal valor "preço do catálogo"
    }
    atendimento_servicos {
        uuid atendimento_id PK, FK
        int servico_preco_id PK, FK
        int ordem "ordem escolhida"
    }
    chaves_idempotencia {
        uuid usuario_id PK
        varchar chave PK
        uuid estabelecimento_id FK
        varchar hash_requisicao
        int status_resposta "NULL = em processamento"
        text corpo_resposta
        timestamptz criado_em "expira em 24 h"
    }
```

Tabelas não detalhadas no diagrama: `comissoes_lavador`, `pausas_atendimento` (motivo `LUNCH|RESTROOM|OTHER`), `service_order_checklist` (um por atendimento e tipo), `service_order_photo`, `expense_category`, `cash_transaction` (`INCOME|EXPENSE`), `vendas` (`VENDA|ORCAMENTO`), `venda_itens`, `venda_pagamentos` e `codigos_acesso` (só o hash do código de WhatsApp).

## Restrições que carregam regras de negócio

| Restrição | Regra |
|---|---|
| `ux_atendimentos_veiculo_no_patio`: único em `veiculo_id` quando `status IN (AGUARDANDO_PATIO, EM_LAVAGEM, PAUSADA, PRONTO)` | Um carro só pode ter um atendimento aberto no pátio. Agendamentos futuros não contam |
| `pagamentos.atendimento_id` único | Um pagamento por atendimento, mesmo com clique duplo |
| `uq_checklist_atendimento_tipo` | Um checklist de entrada e um de saída por atendimento (regravar substitui) |
| Telefone, placa e `google_sub` únicos por `estabelecimento_id`; `email` único global | Isolamento entre lava-jatos e login do gerente por e-mail |
| `chaves_idempotencia` PK (`usuario_id`, `chave`) | Uma operação por chave e por usuário |
| `CHECK`s de horário (`hora_abertura < hora_fechamento`) e de capacidade (`max_veiculos_hora > 0`) | Configuração do lava-jato sempre válida |
| `venda_pagamentos.valor > 0`, `venda_itens.quantidade > 0` | Valores coerentes. A soma dos pagamentos não passa do total (validado no caso de uso) |

## Migrações

| Versão | Arquivo | O que faz |
|---|---|---|
| V1 | `V1__reset_legacy_schema.sql` | Remove, uma única vez, o esquema anterior à 1.5.0 (decisão do lançamento multi-lava-jato: dados descartados) |
| V2 | `V2__baseline.sql` | Esquema completo da plataforma multi-lava-jato |
| V3 | `V3__concurrency_and_idempotency.sql` | Índice de um atendimento aberto por veículo no pátio e tabela `chaves_idempotencia` |
| V4 | `V4__audit_trail.sql` | Trilha de auditoria |
| V5 | `V5__service_order_cancellation.sql` | Cancelamento de atendimentos |
| V6 | `V6__service_order_refunds.sql` | Estorno de atendimentos |
| V7 | `V7__sale_refunds.sql` | Estorno de vendas |
| V8 | `V8__single_platform_administrator.sql` | Administrador único da plataforma |
| V9 | `V9__multiple_services_per_service_order.sql` | Associação de um ou mais serviços a cada atendimento; migra os atendimentos existentes |

Para criar uma migração nova, use a próxima versão disponível e não altere as já aplicadas. O app e os testes rodam as mesmas migrações. **Atenção ao subir a V3:** se o banco tiver um carro com dois atendimentos abertos, o índice não é criado e o app não sobe; finalize um deles antes.

## Fuso horário

Datas são gravadas como `TIMESTAMP WITH TIME ZONE` (instantes). Tudo que é dia ou hora do negócio (agenda, horário de funcionamento, slots de capacidade, filtros de relatório, vendas e caixa por dia) é calculado em **`America/Manaus`** (UTC-4), independentemente do fuso do servidor ou do navegador.
