# Multi-tenant: vários lava-jatos em uma instalação

## Modelo

```mermaid
flowchart TB
    P["Plataforma<br/>(uma instalação)"]
    ADM["administradores<br/>(fora de qualquer lava-jato)"]
    P --- ADM
    P --> E1["estabelecimento: Lava Rápido Tom<br/>slug lava-rapido-tom"]
    P --> E2["estabelecimento: Brilho Auto Center<br/>slug brilho-auto-center"]
    E1 --> D1["usuarios · veiculos · servicos_preco · atendimentos<br/>vendas · cash_transaction · expense_category · codigos_acesso"]
    E2 --> D2["usuarios · veiculos · servicos_preco · atendimentos<br/>vendas · cash_transaction · expense_category · codigos_acesso"]
```

- Toda tabela operacional tem `estabelecimento_id`. As demais (pagamentos, comissões, pausas, checklist, fotos, itens e pagamentos de venda) pertencem ao lava-jato pelo atendimento ou pela venda.
- **Unicidade por lava-jato:** telefone, placa e conta Google. O mesmo cliente pode ter conta em dois lava-jatos, e a mesma placa pode existir nos dois.
- **Unicidade na plataforma:** `usuarios.email`. O e-mail identifica de qual lava-jato é o gerente.
- **Administrador da plataforma:** fica na tabela `administradores` e não pertence a nenhum lava-jato. Ele cria e suspende lava-jatos, mas **não acessa** dados operacionais.

## Como a requisição descobre o lava-jato

```mermaid
flowchart TD
    REQ[Requisição /api/**] --> TOK{Tem token válido?}
    TOK -->|sim| ACC["Lava-jato da conta<br/>(AccountAccess.establishmentId)"]
    ACC --> ACT{Lava-jato ativo<br/>e conta ativa?}
    ACT -->|não| R401[401/403]
    ACT -->|sim| SET[TenantContext.set]
    TOK -->|não| PUB{Rota pública que<br/>exige lava-jato?}
    PUB -->|"não (setup, /api/establishments,<br/>tickets, logins da equipe...)"| NOTEN[Segue sem lava-jato]
    PUB -->|"sim (services, establishment,<br/>availability, auth/customers*)"| HDR{Cabeçalho<br/>X-Establishment?}
    HDR -->|ausente| R400["400 Escolha o lava-jato"]
    HDR -->|slug inexistente ou suspenso| R404["404 Lava-jato não encontrado"]
    HDR -->|slug ativo| SET
    SET --> GUARD["TenantResourceGuard:<br/>ids de atendimento, conta ou caixa<br/>de outro lava-jato → 403"]
    GUARD --> H[Handler]
    NOTEN --> H
    H --> SQL["Toda consulta com :tenant<br/>(TenantBindingCustomizer)"]
    SQL --> FIN["finally: TenantContext.clear()"]
```

### Peças

| Peça | Papel |
|---|---|
| `TenantContext` (`data.tenancy`) | `ThreadLocal` com o lava-jato da requisição: `set`, `current`, `require`, `runAs`, `clear` |
| `AuthorizationMiddleware` | Resolve o lava-jato pelo token ou, nas rotas públicas do cliente, pelo cabeçalho `X-Establishment` |
| `TenantBindingCustomizer` (JDBI) | Antes de executar qualquer SQL que contenha `:tenant`, faz o bind com `TenantContext`. Sem lava-jato, lança `TenantRequiredError` (400). Assim, **esquecer o filtro vira erro, não vazamento** |
| `TenantResourceGuard` | Recusa (403) ids de atendimento, conta ou caixa que existem em outro lava-jato, antes do handler |
| `RequestTracing` | Limpa o `TenantContext` no `finally`, para a thread não carregar o lava-jato para a próxima requisição |

### Logins sem lava-jato no cabeçalho

A equipe não escolhe o lava-jato. Ele é identificado pela credencial:

| Login | Como identifica o lava-jato |
|---|---|
| Gerente: e-mail + senha | `usuarios.email` é único na plataforma |
| Lavador: QR do crachá | O hash HMAC do token do crachá aponta para a conta |
| Lavador: telefone + PIN ou senha | O telefone de conta da equipe com **correspondência única** entre lava-jatos ativos. Se o telefone for ambíguo, o login falha |
| Administrador | Tabela `administradores` (sem lava-jato) |

O cliente sempre escolhe o lava-jato antes, na tela inicial (busca por nome em `GET /api/establishments?query=` ou link `/?lavajato=slug` do balcão). O PWA envia a escolha em `X-Establishment`.

## Garantias e onde são testadas

| Garantia | Teste |
|---|---|
| Consultas de um lava-jato não retornam dados de outro | `EstablishmentIntegrationTest` |
| Ids de outro lava-jato → 403 | `AuthorizationMiddlewareTest` |
| Sem lava-jato → 400, slug inexistente ou suspenso → 404 | `AuthorizationMiddlewareTest` |
| Lavador de um lava-jato não entra em outro (crachá, PIN e telefone ambíguo) | Testes de acesso + verificação manual com os dados de demonstração |
| Suspender um lava-jato bloqueia equipe e clientes e preserva os dados | `EstablishmentIntegrationTest` |

## Ciclo de vida de um lava-jato

```mermaid
stateDiagram-v2
    [*] --> Ativo: POST /api/admin/establishments<br/>(gerente inicial + catálogo padrão + categorias de despesa)
    Ativo --> Suspenso: PATCH .../active active=false
    Suspenso --> Ativo: PATCH .../active active=true
    note right of Suspenso
        Equipe e clientes sem acesso,
        some do seletor do cliente.
        Dados preservados.
    end note
```
