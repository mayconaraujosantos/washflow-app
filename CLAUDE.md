# Domínio e Estrutura de Dados (Washflow)

Este documento define as entidades de negócio, os estados do fluxo do veículo (fila lógica) e a estrutura relacional do sistema O banco é somente PostgreSQL (desenvolvimento, testes com PostgreSQL embutido e produção); as regras de fila também valem para repositórios em memória.

> **Git flow:** nunca commite direto em `main` ou `develop`. Use a ferramenta `git flow` (AVH): `feature`/`bugfix` a partir da `develop`, `release` e `hotfix` com tag `vX.Y.Z`, finalizados com `git flow ... finish` (merge local, sem PR). Siga a skill [git-flow](.claude/skills/git-flow/SKILL.md); `make hooks` configura o git flow e o hook `.githooks/pre-commit`, que recusa commits nessas branches.

---

## 1. Máquina de Estados do Veículo (O Washflow)

O ciclo de vida do atendimento é controlado pelo campo `status`; a lavagem pode ser pausada e retomada antes de seguir para `PRONTO`. Antes da lavagem (`AGENDADO` ou `AGUARDANDO_PATIO`) o gerente pode cancelar o atendimento (`CANCELADO`); depois de finalizado, pode estornar o pagamento (`ESTORNADO`). A ordenação da fila do pátio é baseada estritamente no campo `atualizado_em` das tabelas de serviço.

[ AGENDADO ] ➔ [ AGUARDANDO_PATIO ] ➔ [ EM_LAVAGEM ] ⇄ [ PAUSADA ]
                                                   ➔ [ PRONTO ] ➔ [ FINALIZADO ]
[ AGENDADO | AGUARDANDO_PATIO ] ➔ [ CANCELADO ]
[ FINALIZADO ] ➔ [ ESTORNADO ]

---

## 2. Dicionário de Entidades (Estrutura das Tabelas)

### 2.1 Tabela: `usuarios`
Armazena todos os atores do sistema. O acesso às telas do PWA será controlado pelo campo `perfil`.
* `id`: UUID ou BIGINT (Chave Primária)
* `nome`: VARCHAR(100)
* `telefone`: VARCHAR(20) (Usado para login rápido do cliente)
* `senha_hash`: VARCHAR(255)
* `perfil`: VARCHAR(20) — Restrito a: `CLIENTE`, `LAVADOR`, `GERENTE`
* `criado_em`: TIMESTAMP
* `ativo`: BOOLEAN (padrão `TRUE`); contas inativas não autenticam nem usam rotas protegidas

### 2.1.1 Tabela: `estabelecimentos` (multi-lava-jato)
Uma instalação atende vários lava-jatos. Cada um é criado pelo administrador da plataforma.
* `id`: UUID (Chave Primária)
* `slug`: VARCHAR(60) (único; endereço usado no link dos clientes `/?lavajato=<slug>`)
* `nome`: VARCHAR(100)
* `hora_abertura` / `hora_fechamento`: INT (horas inteiras, horário de Manaus; fechamento exclusivo)
* `abre_domingo`: BOOLEAN
* `max_veiculos_hora`: INT (capacidade do pátio por hora)
* `ativo`: BOOLEAN (suspenso = equipe e clientes sem acesso, dados preservados)
* `criado_em` / `atualizado_em`: TIMESTAMP

Todas as tabelas operacionais (`usuarios`, `veiculos`, `servicos_preco`, `atendimentos`, `vendas`, `cash_transaction`, `expense_category`, `codigos_acesso`) têm `estabelecimento_id` (Chave Estrangeira -> `estabelecimentos.id`); as demais pertencem ao lava-jato pelo atendimento ou pela venda. Telefone, placa e conta Google são únicos **dentro** do lava-jato; o e-mail da equipe (`usuarios.email` com perfil diferente de `CLIENTE`) é único na plataforma (o e-mail diz de qual lava-jato é o gerente), e o do cliente é único dentro do lava-jato (V11). `administradores` (`id`, `nome`, `email` único, `senha_hash`) guarda o administrador da plataforma, que não pertence a nenhum lava-jato.

`usuarios.email` (único, minúsculo) é o login da gestão. `usuarios.pin_hash` (BCrypt), `cracha_hash` (HMAC do token do QR), `pin_tentativas` / `pin_bloqueado_ate` guardam o crachá do lavador; `google_sub` liga a conta Google do cliente. `atendimentos.ticket_token` é o link público do ticket e `codigos_acesso` guarda os códigos de acesso (só o hash), cada um para um `telefone` (WhatsApp) ou um `email`. `usuarios.email_recuperacao` (V10) é o e-mail do cliente para códigos de troca de senha (não é único: o cliente tem uma conta por lava-jato); `codigos_senha` guarda o código pendente de cada conta (só o hash HMAC, 10 minutos, 5 tentativas).

### 2.2 Tabela: `veiculos`
Vincula os carros aos seus respectivos donos (clientes).
* `id`: UUID ou BIGINT (Chave Primária)
* `cliente_id`: BIGINT (Chave Estrangeira -> `usuarios.id`, anulável para clientes sem cadastro completo)
* `placa`: VARCHAR(10) (Único / Indexado)
* `modelo`: VARCHAR(50) (Ex: Civic, Onix)
* `cor`: VARCHAR(30)
* `contato_nome` / `contato_telefone`: contato do dono quando o veículo não tem `cliente_id` (entrada registrada pelo gerente)

### 2.3 Tabela: `servicos_preco`
Catálogo de tipos de lavagem oferecidos pelo estabelecimento.
* `id`: INT (Chave Primária)
* `nome`: VARCHAR(50) (Ex: Lavagem Simples, Completa, Higienização)
* `preco`: DECIMAL(10,2)
* `comissao_lavador`: DECIMAL(10,2) (Valor fixo creditado ao lavador)

### 2.4 Tabela: `atendimentos` (A Fila Operacional)
A tabela central que dita o fluxo físico e financeiro do lava-jato.
* `id`: UUID ou BIGINT (Chave Primária)
* `veiculo_id`: BIGINT (Chave Estrangeira -> `veiculos.id`)
* `servico_preco_id`: INT (Chave Estrangeira -> `servicos_preco.id`, primeiro serviço para compatibilidade)
* `lavador_id`: BIGINT (Chave Estrangeira -> `usuarios.id`, anulável até que um lavador assuma o serviço)
* `status`: VARCHAR(25) (Padrão: `AGENDADO`)
* `data_agendamento`: TIMESTAMP (Data/Hora que o cliente escolheu)
* `criado_em`: TIMESTAMP (Momento em que entrou no banco)
* `atualizado_em`: TIMESTAMP (Crucial para ordenação da fila em tempo real)
* `lavagem_iniciada_em`: TIMESTAMP (Início real da lavagem, usado em relatórios)
* `lavagem_concluida_em`: TIMESTAMP (Conclusão real da lavagem, usado em relatórios)
* `previsao_entrega`: TIMESTAMP (opcional, combinada na entrada)
* `observacoes`: VARCHAR(500) (opcional)
* `cancelado_em`, `cancelado_por` (-> `usuarios.id`), `motivo_cancelamento` (`CUSTOMER_GAVE_UP`, `NO_SHOW`, `REGISTERED_BY_MISTAKE`, `OTHER`) e `descricao_cancelamento` (VARCHAR(200), obrigatória para `OTHER`): preenchidos ao cancelar (V5)

`atendimento_servicos` relaciona um ou mais serviços do catálogo ao atendimento, em ordem de seleção. O total cobrado e a comissão são, respectivamente, a soma de `preco` e de `comissao_lavador` dos serviços selecionados; cada atendimento continua reservando uma única vaga.

### 2.5 Tabela: `pagamentos`
Registra o recebimento manual no balcão, no momento em que o gerente finaliza o atendimento.
* `id`: UUID (Chave Primária)
* `atendimento_id`: UUID (Chave Estrangeira única -> `atendimentos.id`)
* `forma_pagamento`: VARCHAR(20) — `CASH`, `PIX`, `DEBIT_CARD` ou `CREDIT_CARD`
* `valor`: DECIMAL(10,2) (Obtido do preço do serviço no catálogo; não aceito do cliente)
* `recebido_em`: TIMESTAMP

### 2.6 Tabela: `comissoes_lavador`
Registra a comissão creditada ao lavador ao finalizar o atendimento.
* `id`: UUID (Chave Primária)
* `atendimento_id`: UUID (Chave Estrangeira -> `atendimentos.id`)
* `lavador_id`: UUID (Chave Estrangeira -> `usuarios.id`)
* `valor`: DECIMAL(10,2)
* `criado_em`: TIMESTAMP

### 2.6.1 Tabela: `estornos_atendimento`
Estorno total de um atendimento finalizado (V6). Somente inserção (trigger recusa `UPDATE`/`DELETE`); o pagamento e a comissão originais nunca são alterados.
* `id`: UUID (Chave Primária); `estabelecimento_id`
* `atendimento_id`: UUID (Chave Estrangeira única -> `atendimentos.id`): um estorno por atendimento
* `valor`: DECIMAL(10,2) (o valor do pagamento registrado; não aceito do cliente)
* `forma_devolucao`: `CASH`, `PIX`, `DEBIT_CARD` ou `CREDIT_CARD` (padrão: a forma do pagamento)
* `motivo`: `CUSTOMER_COMPLAINT`, `WRONG_CHARGE`, `SERVICE_NOT_DONE` ou `OTHER`; `descricao` VARCHAR(200) obrigatória para `OTHER`
* `comissao_estornada`: DECIMAL(10,2) (zero quando o lavador mantém a comissão)
* `criado_por` (-> `usuarios.id`), `criado_em`: TIMESTAMP (data do estorno, usada no relatório)

### 2.7 Tabela: `pausas_atendimento`
Mantém o histórico das pausas feitas pelo lavador durante uma lavagem.
* `id`: UUID (Chave Primária)
* `atendimento_id`: UUID (Chave Estrangeira -> `atendimentos.id`)
* `lavador_id`: UUID (Chave Estrangeira -> `usuarios.id`)
* `motivo`: `LUNCH`, `RESTROOM` ou `OTHER`
* `descricao`: VARCHAR(200) (Obrigatória para `OTHER`)
* `iniciada_em` / `retomada_em`: TIMESTAMP

### 2.8 Tabelas: `vendas`, `venda_itens`, `venda_pagamentos`, `venda_estornos`
Venda rápida no balcão (`tipo = 'VENDA'`) e orçamento (`tipo = 'ORCAMENTO'`), sem passar pelo pátio.
* `vendas`: `id`, `tipo`, `veiculo_id` (opcional), `cliente_nome`, `cliente_telefone`, `subtotal`, `desconto`, `total`, `observacoes`, `criado_por`, `criado_em`
* `venda_itens`: item do catálogo `servicos_preco` com `descricao` e `preco_unitario` congelados, `quantidade` e `total`
* `venda_pagamentos`: pagamentos parciais e em mais de uma forma (`CASH`, `PIX`, `DEBIT_CARD`, `CREDIT_CARD`); a soma nunca passa do total
* `venda_estornos` (V7, somente inserção): `venda_id`, `forma_devolucao`, `valor`, `motivo` (`CUSTOMER_COMPLAINT`, `WRONG_CHARGE`, `SERVICE_NOT_DONE`, `OTHER`), `descricao` (obrigatória para `OTHER`), `criado_por`, `criado_em`. Estorno total ou parcial de venda rápida (orçamento não tem); cada forma devolve no máximo o que entrou nela, menos o já estornado. Os pagamentos nunca mudam

---

## 3. Lógica das Consultas para as Filas (Portabilidade In-Memory)

As migrações ficam em `src/main/resources/db/migration` (histórico em `schema_history`), aplicadas por `JdbiFactory.migrate` no app e nos testes (PostgreSQL embutido, ver `infra/README.md`).

Para garantir que a lógica funcione igual no banco físico ou em listas Java/Kotlin na memória, as listagens devem seguir estas regras:

### Fila de Espera dos Lavadores (Status: AGUARDANDO_PATIO)
* **Regra:** Listar todos os atendimentos onde `status = 'AGUARDANDO_PATIO'` ordenados por `atualizado_em ASC` (Primeiro que chega, primeiro que sai).

### Painel do Gerente (Pátio Geral)
* **Regra:** Listar atendimentos agrupados por status, priorizando `EM_LAVAGEM` e `AGUARDANDO_PATIO` para monitorar gargalos de tempo.
* Os horários de agendamento e funcionamento usam o fuso `America/Manaus` (UTC-4), independentemente do fuso do navegador ou servidor. Dias, horário e capacidade por hora vêm do lava-jato em `estabelecimentos` (padrão: segunda a sábado, 08:00 às 18:00, 3 veículos por hora).

---

## 4. Atores, autorização e operações
Somente clientes podem se cadastrar publicamente. O login de lavador e gerente exige telefone e senha de uma conta provisionada ou promovida por gerente; informar um telefone novo nesses endpoints não concede perfil operacional.
O campo nome é exigido no cadastro de cliente; para entrar (cliente, lavador ou gerente), bastam telefone e senha. A página inicial segue o layout de referência: logo e cartão "Área do cliente" com a escolha "Já tenho conta | Criar conta" no topo (telefone e senha do cliente), "Esqueci a senha", instalação do app e, no rodapé, links para o login do lavador e do gerente. Antes do cartão, o cliente escolhe o lava-jato (busca por nome ou link `/?lavajato=<slug>` do balcão, lembrado no aparelho) e pode trocá-lo; os links da equipe ficam sempre visíveis. Já dentro da conta, o painel do cliente mostra "Lava-jato: <nome>" com "Trocar" (sai da conta e volta à escolha) e o agendamento começa com "Agendando no <nome>". Uma instalação nova vai antes para `/configurar`. O telefone é enviado só com dígitos.

O login exige telefone e senha (mínimo de 8 caracteres); o nome só é exigido para criar a conta de cliente. A senha é armazenada com BCrypt. Após autenticar, a API emite um token HMAC assinado, com expiração de 8 horas e perfil fixado pelo endpoint de login. Rotas operacionais exigem `Authorization: Bearer <token>` e verificam o perfil atual do usuário.

| Perfil | Operações autorizadas |
|---|---|
| `CLIENTE` | Cadastrar/listar seus veículos, agendar e consultar seus próprios atendimentos |
| `LAVADOR` | Consultar fila, iniciar e concluir lavagens atribuídas a si |
| `GERENTE` | Consultar o pátio e a agenda, registrar entradas pela placa, receber veículos, cancelar atendimentos antes da lavagem, registrar pagamento, estornar atendimentos finalizados e finalizar atendimentos, cadastrar lavadores, alterar perfis e ativar/desativar contas |

O painel do gerente lista contas e permite cadastrar lavadores com senha inicial (mínimo de 8 caracteres). A senha é persistida somente como hash BCrypt e deve ser entregue diretamente ao colaborador. Um gerente pode alterar contas entre `CLIENTE`, `LAVADOR` e `GERENTE`, inclusive o próprio perfil; ao alterar o próprio perfil, precisa autenticar novamente pela tela do novo perfil. Contas podem ser desativadas sem exclusão dos dados históricos. A desativação é aplicada no login e em cada request autenticada; lavadores com atendimentos `EM_LAVAGEM` ou `PAUSADA` não podem ser desativados nem mudar de perfil até que esses atendimentos sejam resolvidos.

Endpoints operacionais:
* `GET /api/operations/queue`: atendimentos `AGUARDANDO_PATIO`, ordenados por `atualizado_em ASC`.
* `GET /api/operations/my-orders`: lavagens `EM_LAVAGEM` ou `PAUSADA` atribuídas ao lavador autenticado.
* `GET /api/operations/yard`: atendimentos não finalizados, priorizando `EM_LAVAGEM`, `AGUARDANDO_PATIO`, `PRONTO` e `AGENDADO`.
* As listagens de atendimentos trazem `details` (placa, modelo, cor, cliente, telefone, serviço e lavador) para exibição.
* `POST /api/operations/entries`: o gerente registra a entrada pela placa (`plate`, `servicePriceId`; opcionais `vehicleModel`, `vehicleColor`, `customerName`, `customerPhone`, `expectedDeliveryAt`, `notes`). Placa nova cria o veículo; telefone de cliente cadastrado vincula o veículo à conta, senão fica como contato. Sem `scheduledAt` o atendimento entra direto em `AGUARDANDO_PATIO`; com `scheduledAt` fica `AGENDADO`, respeitando horário de funcionamento e capacidade por hora.
* `GET /api/operations/vehicles?query=`: busca veículos por início da placa, nome ou telefone do dono (mínimo 2 caracteres, até 10 resultados).
* `GET /api/schedule/availability?date=YYYY-MM-DD` (público): horários do dia com vagas restantes (`booked`/`capacity`), já marcando horários lotados, passados ou dia fechado - as telas de agendamento do gerente e do cliente só oferecem horários aceitos pela API.
* `GET /api/operations/agenda?from=YYYY-MM-DD&to=YYYY-MM-DD`: atendimentos por data de agendamento (data Manaus, qualquer status), intervalo máximo de 62 dias.
* `POST /api/vehicles` (cliente): cadastra o carro (`plate` normalizada para letras maiúsculas e dígitos, formato antigo ou Mercosul, senão 400; `model` até 50, `color` até 30 caracteres). Placa já existente no lava-jato → 409, exceto o carro registrado no balcão sem conta (`cliente_id` nulo) cujo telefone de contato é o do cliente: esse passa para a conta dele (mantém modelo/cor quando não informados, apaga o contato) e o histórico de atendimentos vem junto.
* `GET /api/customers/{id}/unclaimed-vehicles` (cliente): carros do balcão com o telefone do cliente e ainda sem conta; a tela "Meus veículos" mostra cada um com o botão "É meu". O formulário sugere o modelo pela tabela FIPE (`vehicle-models.json`, gerado por `scripts/fipe-models.py`; o app nunca chama a FIPE) e oferece a cor em botões.
* `POST /api/service-orders`: cliente agenda um veículo com `servicePriceIds` (um ou mais serviços), `scheduledAt`; um atendimento ocupa uma única vaga, cobra a soma dos preços e credita ao lavador a soma das comissões fixas.
* `POST /api/sales`: registra venda rápida ou orçamento (`type` = `QUICK_SALE` ou `BUDGET`, `items` com `servicePriceId` e `quantity`, `discount`, `payments` com `method` e `amount`). Preços vêm do catálogo; desconto entre zero e o subtotal; pagamentos não excedem o total. A resposta traz `paidAmount` e `outstandingAmount`.
* `GET /api/sales?from=YYYY-MM-DD&to=YYYY-MM-DD[&type=]`: vendas/orçamentos por data Manaus de criação, com `refunds` e `refundedAmount`.
* `POST /api/sales/{id}/refunds` (gerente): estorna `amount` (até 2 casas) na forma `method`, com `reason` e `description`; acima do que a forma ainda tem → 409 ("Pix só tem R$ 20,00 para estornar nesta venda"), orçamento → 409. A venda fica travada (`FOR UPDATE`) durante a conta, então estornos simultâneos nunca passam do pago. Responde a venda atualizada.
* `GET /api/sales/refunds?from=&to=` (gerente): estornos de venda pela data do estorno. A tela "Vendas realizadas" (`/gerente/vendas`) lista as vendas com o botão "Estornar"; o "Caixa do dia" desconta os estornos feitos no dia (atendimentos e vendas, de qualquer data).
* `POST /api/services`: o gerente cadastra produto ou serviço no catálogo (`name`, `price`, `washerCommission`). Somente `GET /api/services` é público.
* Caixa (`/api/cash-transactions`), categorias de despesa (`/api/expense-categories`) e checklist/fotos de atendimento são exclusivos do gerente.
* `PATCH /api/service-orders/{id}/pause`: pausa uma lavagem atribuída ao lavador; exige motivo (`LUNCH`, `RESTROOM`, `OTHER`) e descrição para `OTHER`.
* `PATCH /api/service-orders/{id}/resume`: retoma a lavagem pausada pelo próprio lavador.
* `PATCH /api/service-orders/{id}/cancel` (gerente): cancela atendimento `AGENDADO` ou `AGUARDANDO_PATIO` com `reason` (`CUSTOMER_GAVE_UP`, `NO_SHOW`, `REGISTERED_BY_MISTAKE`, `OTHER`) e `description` (obrigatória para `OTHER`); depois disso, 409. O atendimento não é apagado: sai do pátio, libera a vaga da hora e a placa pode entrar de novo; continua na agenda e no ticket como cancelado.
* `PATCH /api/service-orders/{id}/refund` (gerente): estorna o pagamento total de um atendimento `FINALIZADO` (vira `ESTORNADO`; segundo estorno → 409). Recebe `reason` (`CUSTOMER_COMPLAINT`, `WRONG_CHARGE`, `SERVICE_NOT_DONE`, `OTHER`), `description` (obrigatória para `OTHER`), `refundMethod` (opcional; padrão a forma do pagamento) e `reverseCommission` (padrão `false`: o lavador mantém a comissão). Na tela, o botão "Estornar" fica nos atendimentos finalizados da agenda.
* `PATCH /api/service-orders/{id}/finalize`: recebe `paymentMethod`; em uma transação, muda o status para `FINALIZADO`, registra o pagamento pelo preço do catálogo e credita a comissão.
* `GET /api/operations/report?from=YYYY-MM-DD&to=YYYY-MM-DD`: relatório do gerente por data Manaus, com faturamento, comissões, serviços, pagamentos, pausas e tempo médio de lavagem (descontadas as pausas). Estornos entram na data do estorno (`refundedOrders`, `refunds`, `reversedCommissions`, `refundReasons`), com `netRevenue` e `netCommissions` líquidos; os dias anteriores não mudam. Também traz os cancelamentos do período por motivo (`cancelledOrders`, `cancellationReasons`, pela data do cancelamento) e as vendas rápidas (`salesReceived` pela data do pagamento, `saleRefunds` e `saleRefundReasons` pela data do estorno, `netSales`); intervalo máximo de 366 dias.
* `GET /api/users`: lista as contas para o gerente.
* `POST /api/users`: cria uma conta `LAVADOR` com `name`, `phone` e `password`.
* `PATCH /api/users/{id}/profile`: altera o perfil para `CUSTOMER`, `WASHER` ou `MANAGER`.
* `PATCH /api/users/{id}/active`: recebe `{"active": false}` para desativar ou `true` para reativar a conta.
* `PATCH /api/users/{id}/password` (gerente): recebe `password` (mínimo 8) e define a nova senha de um cliente ou lavador do próprio lava-jato que perdeu a sua; conta de gerente → 403 (quem redefine é o administrador). Na tela "Equipe e acessos", botão "Redefinir senha"; o "Esqueci a senha" do cliente orienta a pedir a nova senha no balcão.
* "Esqueci a senha" por e-mail (públicos): `POST /api/auth/customers/password-reset` (`phone`, dentro do lava-jato) e `POST /api/auth/managers/password-reset` (`email` de acesso) enviam um código de 6 dígitos — para o `email_recuperacao` do cliente (ou o e-mail da conta Google) e para o e-mail de login do gerente — e respondem 202 `{expiresInSeconds}` igual exista a conta ou não (reenvio só após 60 s, em silêncio). `.../password-reset/confirm` (`code`, `password` com mínimo 8) troca a senha: 200, ou 401 para código errado/expirado (contado pelo `LoginThrottle`, que zera o bloqueio do login ao trocar). Sem SMTP: 503. `GET /api/auth/options` informa `emailCode`.
* `GET`/`PATCH /api/customers/{id}/recovery-email` (o próprio cliente): `{email}` da tela "Meus dados" (`/cliente/dados`, ícone de pessoa no topo do painel); o cadastro também aceita o e-mail, opcional.

O cliente mantém o painel aberto com atualização automática a cada 3 segundos. Quando o status é `PRONTO`, o painel mostra uma orientação persistente para retirada enquanto o atendimento não for finalizado. Não há envio de push com o app fechado.

### Plataforma, lava-jatos e primeiro acesso
* **Isolamento:** cada requisição roda dentro de um lava-jato (`TenantContext`): o da conta autenticada ou, nas telas públicas do cliente, o escolhido na tela inicial (cabeçalho `X-Establishment: <slug>`). Toda consulta usa `estabelecimento_id = :tenant`, preenchido automaticamente pelo `TenantBindingCustomizer` do JDBI; sem lava-jato a consulta falha (400 "Escolha o lava-jato"). O middleware ainda recusa (403) ids de atendimento, conta ou caixa de outro lava-jato.
* Catálogo, `/api/establishment`, agenda pública e login/cadastro de cliente exigem o lava-jato (400 sem cabeçalho, 404 para endereço inexistente ou suspenso). Logins da equipe dispensam: e-mail do gerente, crachá e PIN do lavador (o PIN entra na conta de lavador, com aquele telefone, cujo crachá ele confere) identificam o lava-jato.
* `GET /api/setup` (público) informa se a plataforma já tem administrador; a página inicial redireciona para `/configurar` enquanto não tiver.
* `POST /api/setup` (público, uma única vez: `ownerName`, `ownerEmail`, `ownerPassword`, `setupCode`) cria o administrador da plataforma e devolve a sessão (perfil `ADMIN`). Em produção exige o `SETUP_CODE`.
* `POST /api/auth/admins` (e-mail e senha) e a página `/login/admin`; o painel `/admin` lista os lava-jatos, cria (`POST /api/admin/establishments`: nome, horário, capacidade e gerente inicial com e-mail e senha; cria também o catálogo inicial de serviços e categorias de despesa) e suspende/reativa (`PATCH /api/admin/establishments/{id}/active`). `PATCH /api/admin/establishments/{id}/manager-password` (`password`, mínimo 8) define a nova senha do gerente mostrado no cartão ("Redefinir senha do gerente"). Rotas `/api/admin/**` são exclusivas do administrador, que não acessa dados de lava-jato.
* `GET /api/establishments?query=` (público): lava-jatos ativos por nome, para o seletor do cliente.
* `GET /api/establishment` (público, dentro do lava-jato) e `PUT /api/establishment` (gerente): nome, horário, domingos e capacidade do pátio.
* O gerente entra com e-mail e senha (`POST /api/auth/managers` com `email` ou, para contas antigas, `phone`). `PATCH /api/users/{id}/email` define o e-mail de acesso.

### Acesso por perfil
* **Gerente:** e-mail + senha (contas antigas ainda entram pelo telefone). "Esqueci a senha" no login envia o código ao e-mail de acesso; sem SMTP, o administrador redefine pelo painel.
* **Lavador:** crachá emitido pelo gerente (`POST /api/users/{id}/badge`, mostrado uma única vez): telefone + PIN de 6 dígitos (`POST /api/auth/washers/pin`; 5 erros bloqueiam por 15 minutos; o mesmo telefone pode ter crachá em mais de um lava-jato e cada PIN abre a sua conta) ou leitura do QR do crachá (`POST /api/auth/washers/badge`; o QR abre `/login/lavador?cracha=...`). Gerar de novo invalida o crachá anterior. A senha do lavador passou a ser opcional.
* **Cliente presencial:** o gerente gera o ticket do atendimento (`POST /api/service-orders/{id}/ticket`), entregue por QR, link ou WhatsApp; `GET /api/tickets/{token}` e a página `/ticket/:token` mostram veículo, serviço e andamento sem login e sem dados do cliente.
* **Cliente com conta:** telefone + senha, código de 6 dígitos por WhatsApp ou e-mail (`POST /api/auth/customers/otp` e `/otp/verify` com `phone` ou `email`; vale 10 minutos, 5 tentativas, reenvio após 60 s, ou na hora se o envio falhou; os links `/entrar/whatsapp?telefone=...&codigo=...` e `/entrar/email?email=...&codigo=...` funcionam como magic link) ou Google (`POST /api/auth/customers/google`). Telefone/e-mail/conta Google novos viram conta de cliente; contas da equipe nunca entram por esses caminhos. `GET /api/auth/options` informa o que está habilitado (`whatsappCode` por `OTP_CHANNEL`, `emailCode` por `EMAIL_CHANNEL`, `googleClientId`). "Esqueci a senha" envia o código ao e-mail de "Meus dados"; sem e-mail cadastrado, o gerente define uma nova senha no balcão.

### Concorrência e idempotência
Cliques duplos, reenvios após timeout e duas pessoas agindo ao mesmo tempo não podem duplicar nada (histórico em `docs/testes-mundo-real.md`):
* **Transições de status** são compare-and-set: o `UPDATE` só grava se o atendimento ainda estiver no status esperado (`status = :expected`); quem perde a corrida recebe 409. Ex.: dois lavadores iniciando o mesmo carro, finalizar com clique duplo (um único pagamento).
* **Capacidade por hora** é verificada e reservada de forma atômica (`createWithinCapacity`, trava `pg_advisory_xact_lock` por lava-jato e hora): agendamentos simultâneos nunca passam de `max_veiculos_hora`.
* **Um atendimento aberto por veículo no pátio** (`AGUARDANDO_PATIO`, `EM_LAVAGEM`, `PAUSADA`, `PRONTO`), garantido pelo índice único `ux_atendimentos_veiculo_no_patio`: nova entrada da mesma placa é recusada com 409 até o atendimento atual ser finalizado. Agendamentos futuros (`AGENDADO`) não contam.
* **Violação de unicidade** do banco (SQLState 23505) vira `DuplicateKeyError`, nunca 500: cadastro de cliente enviado duas vezes entra na conta recém-criada (mesma senha) e lavador duplicado vira 409.
* **`Idempotency-Key`** (tabela `chaves_idempotencia`): todo `POST` autenticado que traz o cabeçalho grava a primeira resposta de sucesso por 24 horas, por usuário; o reenvio com a mesma chave devolve a mesma resposta (cabeçalho `Idempotent-Replayed: true`) sem executar de novo. Mesma chave com outro conteúdo → 422; mesma chave enquanto a primeira ainda roda → 409; falhas liberam a chave. O PWA (`FetchHttpClient`) envia a chave em todo POST autenticado, reaproveita a requisição idêntica em andamento e mantém a chave para o reenvio após uma falha.
* Testes: `ConcurrencyIntegrationTest` (corridas reais no PostgreSQL embutido), `IdempotencyGuardTest`, `JdbiIdempotencyStoreIntegrationTest`; contra um servidor rodando, `scripts/probe_real_world.py`.

### Auditoria
* Toda alteração autenticada que dá certo (`POST`, `PUT`, `PATCH`, `DELETE` com 2xx) é gravada pelo `AuditTrail` na tabela `auditoria` (V4): lava-jato (NULL para o administrador da plataforma), usuário, perfil, método, rota (`/api/cash-transactions/{id}`), caminho real, status, corpo da requisição e horário. Endpoints novos entram automaticamente.
* O corpo é guardado com senhas, PINs, tokens, códigos e imagens trocados por `***`, cortado em 2000 caracteres; corpo que não é JSON não é guardado.
* Roda dentro do `IdempotencyGuard` (reenvio repetido não grava duas vezes) e fora da transação da operação: se a gravação falhar, a operação vale e o erro vai para o log.
* `auditoria` é somente inserção: um trigger recusa `UPDATE` e `DELETE`.
* `GET /api/audit?from=YYYY-MM-DD&to=YYYY-MM-DD` (gerente): registros do próprio lava-jato por data Manaus, mais recentes primeiro, com o nome do autor; intervalo máximo de 92 dias.

### Observabilidade
* Logs em stdout (Logback, `src/main/resources/logback.xml`, `LOG_LEVEL`), uma linha `logger=http` por requisição com `request_id`, rota, status, duração, usuário e motivo do erro (4xx em WARN, 5xx em ERROR com stack trace).
* Cada requisição tem um ID (`X-Request-Id`), devolvido também como `requestId` nos corpos de erro (`HttpHelper.errorBody`); o app mostra o código nas falhas 5xx.
* `GET /api/metrics`: métricas Prometheus (HTTP por rota, pool Hikari, JVM), exige `Authorization: Bearer METRICS_TOKEN`; sem a variável, responde 404.
* `POST /api/client-errors` (público, limitado a 20 por minuto por IP): erros do navegador enviados pelo `ErrorReporter` e pelo `ErrorBoundary` do PWA.
* Toda resposta (API, PWA e arquivos estáticos) leva `SecurityHeaders`: HSTS, CSP (só o próprio app e o Google Identity Services; imagens `data:`), `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy` e `Permissions-Policy` (câmera só para o leitor de QR). `/swagger` fica sem CSP por causa dos scripts inline.

### Bootstrap e limites de autenticação
* Em produção, configure `AUTH_TOKEN_SECRET` (mínimo 32 caracteres).
* Login por senha ou PIN (`/api/auth/customers`, `/washers`, `/washers/pin`, `/managers`, `/admins`) é limitado pelo `LoginThrottle`: 5 erros (401) na mesma conta (rota + lava-jato + telefone/e-mail) ou 30 erros do mesmo IP (último salto do `X-Forwarded-For`) bloqueiam por 15 minutos com 429 e `Retry-After`; um login certo zera a conta. Contadores em memória, por réplica.
* O gerente inicial sem senha pode ser ativado uma única vez com `BOOTSTRAP_MANAGER_PHONE` e `BOOTSTRAP_MANAGER_PASSWORD`; após definir a senha, o login usa o hash BCrypt persistido. Em instalações novas, use o assistente `/configurar` (com `SETUP_CODE` em produção) e crie os lava-jatos pelo painel `/admin`.
* Contas preexistentes sem `senha_hash` não podem entrar em produção até que sua senha seja configurada por um processo administrativo seguro. Apenas ambiente de desenvolvimento permite reivindicar uma conta sem hash no primeiro login.
* A senha comprova acesso à conta após o cadastro, mas o telefone só é verificado no login por código de WhatsApp. O envio real por WhatsApp exige integrar um provedor em `CustomerModule.whatsappSender` (hoje só existe o canal `log`, de desenvolvimento).
* E-mail tem um único canal (`UserModule.emailSender`), usado pelo código de acesso do cliente (`EmailOtpSender`) e pelos códigos de troca de senha: `EMAIL_CHANNEL=smtp` envia pelo `SmtpEmailSender` (Jakarta Mail) com `SMTP_HOST`, `SMTP_PORT`, `SMTP_SECURITY` (`ssl`, `starttls` ou `none`), `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`, `SMTP_FROM_NAME`; `log` escreve o e-mail no log (só fora de produção); `none` desliga e o app esconde as opções (`emailCode`). Sem `EMAIL_CHANNEL`, vale `smtp` se `SMTP_HOST` existe, senão `log` em desenvolvimento e `none` em produção. Localmente, o serviço `mailpit` do `docker-compose.yml` recebe os e-mails em http://localhost:8025. Falha de envio vira 503 sem expor o motivo (que vai para o log).

---

## 5. Organização do código por agregado

As camadas da Clean Architecture (`domain`, `data`, `application`, `infra`, `presentation`) são divididas por agregado, não por operação. Arquivo novo entra no agregado a que pertence:

| Agregado | O que reúne |
|---|---|
| `serviceorder` | ciclo de vida do atendimento: entrada no status, lavagem, pausa, finalização, cancelamento, estorno, checklist e fotos |
| `schedule` | agenda, disponibilidade e agendamento |
| `yard` | pátio: entrada pela placa, fila de espera e lavagens em andamento |
| `sale` | venda rápida, orçamento e estorno de venda |
| `finance` | caixa, lançamentos, categorias de despesa, relatório do caixa e relatório operacional |
| `vehicle` | veículos, busca e vínculo com o cliente |
| `user` | contas, equipe, login, crachá do lavador, perfil, e-mail e status ativo |
| `establishment` | lava-jato (tenant), configuração inicial e plataforma |
| `customer` | visão do cliente: login sem senha (código e Google), seus atendimentos e ticket |
| `pricing` | catálogo de serviços e preços |
| `shared` | o que mais de um agregado usa (`SchedulingRules`, `Secrets`, `PaymentMethod` e `ContactRules` no domínio, saúde do banco, catálogo nas portas de persistência) |

Evite agregado com uma classe só: junte ao agregado mais próximo. Pacotes: `domain/entities/<agregado>`, `domain/usecases/<agregado>`, `domain/errors/<agregado>` (mais `auth` e `common`), `data/usecases/<agregado>`, `data/protocols/db/<agregado>`, `presentation/controllers/<agregado>` (mais `system` para o health check) e `infra/db/jdbi/<agregado>` (com `JdbiFactory`, `UniqueViolations`, saúde do banco, idempotência e auditoria na raiz). A montagem fica em `application/modules`: um módulo por agregado (`ServiceOrderModule`, `YardModule`...) com um método por caso de uso e por controller; as rotas pedem o controller ao módulo. Os testes espelham o pacote da classe testada. Erros de negócio estendem uma categoria de `DomainError` (`domain/errors/common`: `ValidationError`, `AuthenticationError`, `AccessDeniedError`, `NotFoundError`, `ConflictError`, `TooManyRequestsError`, `UnavailableError`), são unchecked e têm um `code` estável (`SERVICE_ORDER_NOT_CANCELLABLE`); o `HttpHelper` traduz a categoria em status (400, 401, 403, 404, 409, 429, 503) e responde `{error, code, requestId}`, então o controller não precisa de `catch` para eles (o `RequestTracing` cobre os lançados fora do controller). Erro novo entra em `domain/errors/<agregado>` estendendo a categoria. Controllers leem o corpo da requisição só pelo `BodyValues` (`text`, `integer`, `decimal`, `bool`, `uuid`, `objects`, `createdBy`): tipo de JSON errado vira 400, nunca `ClassCastException`/500. O plano completo está em `docs/refactor-by-aggregate.md`.
