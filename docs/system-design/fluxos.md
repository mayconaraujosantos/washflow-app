# Fluxos principais

Diagramas de sequência dos fluxos de ponta a ponta. Os endpoints estão em [api.md](api.md), os status em [maquina-de-estados.md](maquina-de-estados.md) e as regras de acesso em [seguranca-e-acesso.md](seguranca-e-acesso.md).

## Visão geral da operação

```mermaid
flowchart LR
    subgraph Cliente
        A["Escolhe o lava-jato<br/>(/?lavajato=slug ou busca)"] --> B[Entra ou cria conta]
        B --> C[Escolhe serviço, veículo e horário livre]
        C --> D["AGENDADO"]
        D -.-> P1["Painel: atualiza a cada 3 s"]
    end
    subgraph Gerente
        E["Carro chega sem agendamento:<br/>entrada pela placa"] --> F["AGUARDANDO_PATIO"]
        D --> G[Agenda do dia → recebe o carro] --> F
        K["Valida a lavagem, recebe o pagamento"] --> L["FINALIZADO<br/>pagamento + comissão"]
        T["Gera ticket (QR / link / WhatsApp)"] -.-> PT["/ticket/:token"]
    end
    subgraph Lavador
        F --> H["Fila (atualizado_em ASC)"] --> I["Inicia: EM_LAVAGEM"]
        I <--> J["PAUSADA (almoço, banheiro, outro)"]
        I --> K0["Conclui: PRONTO"] --> K
    end
    K0 -.-> P2["Painel do cliente: orientação de retirada"]
```

## 1. Primeira instalação e criação de um lava-jato

```mermaid
sequenceDiagram
    autonumber
    actor Dono as Dono da plataforma
    participant PWA
    participant API
    participant DB as PostgreSQL

    Dono->>PWA: abre /
    PWA->>API: GET /api/setup
    API-->>PWA: {configured: false}
    PWA->>PWA: redireciona para /configurar
    Dono->>PWA: nome, e-mail, senha, código de setup
    PWA->>API: POST /api/setup
    API->>API: SETUP_CODE confere (produção)
    API->>DB: INSERT administradores (trava: só uma vez)
    API-->>PWA: sessão ADMIN
    PWA->>PWA: vai para /admin
    Dono->>PWA: novo lava-jato: nome, horário, capacidade, gerente (e-mail + senha)
    PWA->>API: POST /api/admin/establishments
    API->>DB: estabelecimento + slug + gerente + catálogo padrão + categorias de despesa
    API-->>PWA: lava-jato + link /?lavajato=slug
    Dono-->>Dono: entrega ao gerente o e-mail, a senha e o link/QR dos clientes
```

## 2. Cliente: escolha do lava-jato, cadastro e login

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant PWA
    participant API

    alt link do balcão
        C->>PWA: abre /?lavajato=lava-rapido-tom
        PWA->>PWA: adoptEstablishmentFromUrl → localStorage
    else busca
        C->>PWA: digita o nome
        PWA->>API: GET /api/establishments?query=tom
        API-->>PWA: lava-jatos ativos
        C->>PWA: escolhe
    end
    PWA->>API: GET /api/establishment (X-Establishment: slug)
    API-->>PWA: nome e horário (404 → limpa a escolha e pede de novo)
    alt Criar conta
        C->>PWA: nome, telefone, senha
        PWA->>API: POST /api/auth/customers (X-Establishment)
        API-->>PWA: conta criada + token CLIENTE
    else Entrar
        C->>PWA: telefone, senha
        PWA->>API: POST /api/auth/customers
        API-->>PWA: token, ou 400 "Não encontramos uma conta com este telefone neste lava-jato..."
    else WhatsApp
        PWA->>API: POST /api/auth/customers/otp
        API-->>C: código de 6 dígitos + magic link
        C->>PWA: código (ou abre /entrar/whatsapp?telefone=&codigo=)
        PWA->>API: POST /api/auth/customers/otp/verify
        API-->>PWA: token (cria a conta se for novo e trouxer nome)
    else Google
        PWA->>API: POST /api/auth/customers/google (ID token)
        API-->>PWA: token
    end
```

Se o mesmo cadastro for enviado duas vezes ao mesmo tempo, as duas respostas entram na mesma conta (o índice único barra a segunda conta e o caso de uso faz o login).

## 3. Login da equipe

```mermaid
sequenceDiagram
    autonumber
    actor G as Gerente
    actor L as Lavador
    participant PWA
    participant API
    participant DB as PostgreSQL

    G->>PWA: /login/gerente: e-mail + senha
    PWA->>API: POST /api/auth/managers
    API->>DB: usuarios por e-mail (único) em lava-jato ativo
    API-->>PWA: token GERENTE (o lava-jato vem da conta)

    G->>PWA: Equipe → emitir crachá
    PWA->>API: POST /api/users/{id}/badge
    API->>DB: novo pin_hash + cracha_hash (invalida o anterior)
    API-->>PWA: PIN e QR (mostrados uma vez)

    alt QR do crachá
        L->>PWA: lê o QR → /login/lavador?cracha=...
        PWA->>API: POST /api/auth/washers/badge
    else telefone + PIN
        L->>PWA: telefone + PIN
        PWA->>API: POST /api/auth/washers/pin
        API->>DB: conta da equipe com esse telefone (precisa ser única)
        API->>API: 5 erros → bloqueio de 15 min
    end
    API-->>PWA: token LAVADOR
```

## 4. Entrada de veículo pela placa (balcão)

```mermaid
sequenceDiagram
    autonumber
    actor G as Gerente
    participant PWA
    participant API
    participant UC as DbRegisterVehicleEntry
    participant DB as PostgreSQL

    G->>PWA: placa (busca: GET /api/operations/vehicles?query=)
    G->>PWA: serviço, modelo, cor, nome, telefone, previsão, observações
    PWA->>API: POST /api/operations/entries + Idempotency-Key
    API->>UC: register(params)
    UC->>UC: normaliza placa (ABC1234 / ABC1D23) e telefone
    UC->>DB: serviço existe no catálogo?
    opt com scheduledAt
        UC->>UC: horário de funcionamento + checagem rápida de capacidade
    end
    UC->>DB: veículo pela placa, ou cria (vincula à conta do cliente se o telefone for de um cliente)
    alt sem horário
        UC->>DB: INSERT atendimento AGUARDANDO_PATIO
        DB-->>UC: viola ux_atendimentos_veiculo_no_patio?
        UC-->>API: 409 "Este veículo já está no pátio..."
    else com horário
        UC->>DB: createWithinCapacity (trava por lava-jato + hora)
        UC-->>API: 409 se a hora lotou
    end
    API-->>PWA: atendimento com details (placa, cliente, serviço)
```

## 5. Agendamento com capacidade

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant PWA
    participant API
    participant DB as PostgreSQL

    C->>PWA: escolhe o dia
    PWA->>API: GET /api/schedule/availability?date=2026-11-05
    API->>DB: COUNT por hora do dia (Manaus)
    API-->>PWA: horas com booked/capacity e available (lotada, passada, dia fechado)
    C->>PWA: veículo, serviço, hora livre
    PWA->>API: POST /api/service-orders + Idempotency-Key
    API->>API: horário de funcionamento, 30 min de antecedência, veículo é do cliente
    API->>DB: BEGIN
    API->>DB: pg_advisory_xact_lock(lava-jato | hora)
    API->>DB: COUNT agendados na hora
    alt count < max_veiculos_hora
        API->>DB: INSERT AGENDADO
        API->>DB: COMMIT
        API-->>PWA: 200 AGENDADO
    else lotado
        API->>DB: ROLLBACK
        API-->>PWA: 409 horário lotado
    end
```

Pedidos simultâneos para a mesma hora passam pela trava um de cada vez, e a capacidade nunca é excedida. Horas diferentes ou lava-jatos diferentes não se bloqueiam.

## 6. Lavagem: iniciar, pausar, retomar, concluir

```mermaid
sequenceDiagram
    autonumber
    actor L as Lavador
    participant PWA
    participant API
    participant DB as PostgreSQL

    PWA->>API: GET /api/operations/queue
    API-->>PWA: AGUARDANDO_PATIO por atualizado_em ASC
    L->>PWA: Iniciar
    PWA->>API: PATCH /api/service-orders/{id}/start {washerId}
    API->>DB: UPDATE ... SET EM_LAVAGEM, lavador_id WHERE status = 'AGUARDANDO_PATIO'
    alt 1 linha alterada
        API-->>PWA: 200 (o carro é deste lavador)
    else 0 linhas (outro lavador foi mais rápido)
        API-->>PWA: 409 "...found WASHING"
    end
    L->>PWA: Pausar (almoço)
    PWA->>API: PATCH /{id}/pause {reason: LUNCH}
    API->>DB: PAUSADA + pausas_atendimento (mesma transação)
    L->>PWA: Retomar
    PWA->>API: PATCH /{id}/resume
    API->>DB: EM_LAVAGEM + retomada_em
    L->>PWA: Concluir
    PWA->>API: PATCH /{id}/complete
    API->>DB: PRONTO + lavagem_concluida_em (WHERE status = 'EM_LAVAGEM')
```

## 7. Receber e finalizar

```mermaid
sequenceDiagram
    autonumber
    actor G as Gerente
    participant PWA
    participant API
    participant DB as PostgreSQL

    G->>PWA: Receber (PIX)
    PWA->>API: PATCH /api/service-orders/{id}/finalize {paymentMethod: PIX}
    API->>DB: preço e comissão do catálogo
    API->>DB: BEGIN
    API->>DB: UPDATE status = FINALIZADO WHERE status = 'PRONTO'
    alt 1 linha
        API->>DB: INSERT pagamentos (único por atendimento)
        API->>DB: INSERT comissoes_lavador
        API->>DB: COMMIT
        API-->>PWA: 200 FINALIZADO
    else 0 linhas (clique duplo / já finalizado)
        API->>DB: ROLLBACK
        API-->>PWA: 409
    end
```

## 8. Ticket do cliente presencial

```mermaid
sequenceDiagram
    autonumber
    actor G as Gerente
    actor C as Cliente sem conta
    participant PWA
    participant API

    G->>PWA: Ticket no card do pátio
    PWA->>API: POST /api/service-orders/{id}/ticket
    API-->>PWA: token (o mesmo se já existir)
    PWA-->>G: QR, link ou WhatsApp com /ticket/{token}
    C->>PWA: abre /ticket/{token}
    loop a cada poucos segundos
        PWA->>API: GET /api/tickets/{token}
        API-->>PWA: veículo, serviço, status (sem dados pessoais)
    end
```

## 9. Venda rápida e orçamento, com reenvio seguro

```mermaid
sequenceDiagram
    autonumber
    actor G as Gerente
    participant PWA as PWA (FetchHttpClient)
    participant IG as IdempotencyGuard
    participant API as Venda (caso de uso)
    participant DB as PostgreSQL

    G->>PWA: itens, desconto, pagamentos → Registrar
    PWA->>PWA: chave nova (ou a mesma de uma tentativa que falhou)
    PWA->>IG: POST /api/sales + Idempotency-Key: k1
    IG->>DB: claim(gerente, k1)
    IG->>API: executa
    API->>DB: preços do catálogo, desconto ≤ subtotal, pagamentos ≤ total
    API->>DB: vendas + venda_itens + venda_pagamentos
    API-->>IG: 200 {id, paidAmount, outstandingAmount}
    IG->>DB: guarda a resposta de k1
    IG--xPWA: resposta perdida (rede caiu)
    G->>PWA: Registrar de novo
    PWA->>IG: POST /api/sales + Idempotency-Key: k1 (mesmo corpo)
    IG->>DB: claim → já respondida
    IG-->>PWA: mesma resposta + Idempotent-Replayed: true
    Note over PWA,DB: uma venda só no banco e no caixa
```

Um clique duplo nem chega a sair do navegador duas vezes: o `FetchHttpClient` reaproveita a requisição idêntica que ainda está em andamento.

## 10. Erro na tela → investigação

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário
    participant PWA
    participant API
    participant LOG as Logs (Render)

    PWA->>API: requisição
    API->>LOG: ERROR request_id=a1b2c3d4e5f6 ... stack trace
    API-->>PWA: 500 {error, requestId: a1b2c3d4e5f6}
    PWA-->>U: "Erro no servidor... informe o código a1b2c3d4e5f6"
    U-->>U: envia o código ao suporte
    Note over LOG: busca por request_id=a1b2c3d4e5f6
    PWA->>API: (erro de JavaScript) POST /api/client-errors
    API->>LOG: logger=client Browser error ...
```
