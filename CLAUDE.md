# Domínio e Estrutura de Dados (Washflow)

Este documento define as entidades de negócio, os estados do fluxo do veículo (fila lógica) e a estrutura relacional do sistema para garantir portabilidade entre bancos de dados físicos (ex: PostgreSQL) e ambientes de teste *In-Memory* (ex: H2 / Repositórios em memória).

---

## 1. Máquina de Estados do Veículo (O Washflow)

O ciclo de vida do atendimento é controlado pelo campo `status`; a lavagem pode ser pausada e retomada antes de seguir para `PRONTO`. A ordenação da fila do pátio é baseada estritamente no campo `atualizado_em` das tabelas de serviço.

[ AGENDADO ] ➔ [ AGUARDANDO_PATIO ] ➔ [ EM_LAVAGEM ] ⇄ [ PAUSADA ]
                                                   ➔ [ PRONTO ] ➔ [ FINALIZADO ]

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

### 2.2 Tabela: `veiculos`
Vincula os carros aos seus respectivos donos (clientes).
* `id`: UUID ou BIGINT (Chave Primária)
* `cliente_id`: BIGINT (Chave Estrangeira -> `usuarios.id`, anulável para clientes sem cadastro completo)
* `placa`: VARCHAR(10) (Único / Indexado)
* `modelo`: VARCHAR(50) (Ex: Civic, Onix)
* `cor`: VARCHAR(30)

### 2.3 Tabela: `servicos_preco`
Catálogo de tipos de lavagem oferecidos pelo estabelecimento.
* `id`: INT (Chave Primária)
* `nome`: VARCHAR(50) (Ex: Lavagem Simples, Completa, Higienização)
* `preco`: DECIMAL(10,2)
* `comissao_lavador`: DECIMAL(10,2) (Valor fixo ou percentual que vai para o lavador)

### 2.4 Tabela: `atendimentos` (A Fila Operacional)
A tabela central que dita o fluxo físico e financeiro do lava-jato.
* `id`: UUID ou BIGINT (Chave Primária)
* `veiculo_id`: BIGINT (Chave Estrangeira -> `veiculos.id`)
* `servico_preco_id`: INT (Chave Estrangeira -> `servicos_preco.id`)
* `lavador_id`: BIGINT (Chave Estrangeira -> `usuarios.id`, anulável até que um lavador assuma o serviço)
* `status`: VARCHAR(25) (Padrão: `AGENDADO`)
* `data_agendamento`: TIMESTAMP (Data/Hora que o cliente escolheu)
* `criado_em`: TIMESTAMP (Momento em que entrou no banco)
* `atualizado_em`: TIMESTAMP (Crucial para ordenação da fila em tempo real)
* `lavagem_iniciada_em`: TIMESTAMP (Início real da lavagem, usado em relatórios)
* `lavagem_concluida_em`: TIMESTAMP (Conclusão real da lavagem, usado em relatórios)

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

### 2.7 Tabela: `pausas_atendimento`
Mantém o histórico das pausas feitas pelo lavador durante uma lavagem.
* `id`: UUID (Chave Primária)
* `atendimento_id`: UUID (Chave Estrangeira -> `atendimentos.id`)
* `lavador_id`: UUID (Chave Estrangeira -> `usuarios.id`)
* `motivo`: `LUNCH`, `RESTROOM` ou `OTHER`
* `descricao`: VARCHAR(200) (Obrigatória para `OTHER`)
* `iniciada_em` / `retomada_em`: TIMESTAMP

---

## 3. Lógica das Consultas para as Filas (Portabilidade In-Memory)

Para garantir que a lógica funcione igual no banco físico ou em listas Java/Kotlin na memória, as listagens devem seguir estas regras:

### Fila de Espera dos Lavadores (Status: AGUARDANDO_PATIO)
* **Regra:** Listar todos os atendimentos onde `status = 'AGUARDANDO_PATIO'` ordenados por `atualizado_em ASC` (Primeiro que chega, primeiro que sai).

### Painel do Gerente (Pátio Geral)
* **Regra:** Listar atendimentos agrupados por status, priorizando `EM_LAVAGEM` e `AGUARDANDO_PATIO` para monitorar gargalos de tempo.
* Os horários de agendamento e funcionamento usam o fuso `America/Manaus` (UTC-4), independentemente do fuso do navegador ou servidor. O funcionamento é de segunda a sábado, das 08:00 às 18:00.

---

## 4. Atores, autorização e operações
Somente clientes podem se cadastrar publicamente. O login de lavador e gerente exige telefone e senha de uma conta provisionada ou promovida por gerente; informar um telefone novo nesses endpoints não concede perfil operacional.
O campo nome é exigido no cadastro de cliente; para entrar como lavador ou gerente, bastam telefone e senha.

O login exige telefone, nome e senha (mínimo de 8 caracteres). A senha é armazenada com BCrypt. Após autenticar, a API emite um token HMAC assinado, com expiração de 8 horas e perfil fixado pelo endpoint de login. Rotas operacionais exigem `Authorization: Bearer <token>` e verificam o perfil atual do usuário.

| Perfil | Operações autorizadas |
|---|---|
| `CLIENTE` | Cadastrar/listar seus veículos, agendar e consultar seus próprios atendimentos |
| `LAVADOR` | Consultar fila, iniciar e concluir lavagens atribuídas a si |
| `GERENTE` | Consultar o pátio, receber veículos, registrar pagamento e finalizar atendimentos, cadastrar lavadores, alterar perfis e ativar/desativar contas |

O painel do gerente lista contas e permite cadastrar lavadores com senha inicial (mínimo de 8 caracteres). A senha é persistida somente como hash BCrypt e deve ser entregue diretamente ao colaborador. Um gerente pode alterar contas entre `CLIENTE`, `LAVADOR` e `GERENTE`, inclusive o próprio perfil; ao alterar o próprio perfil, precisa autenticar novamente pela tela do novo perfil. Contas podem ser desativadas sem exclusão dos dados históricos. A desativação é aplicada no login e em cada request autenticada; lavadores com atendimentos `EM_LAVAGEM` ou `PAUSADA` não podem ser desativados nem mudar de perfil até que esses atendimentos sejam resolvidos.

Endpoints operacionais:
* `GET /api/operations/queue`: atendimentos `AGUARDANDO_PATIO`, ordenados por `atualizado_em ASC`.
* `GET /api/operations/my-orders`: lavagens `EM_LAVAGEM` ou `PAUSADA` atribuídas ao lavador autenticado.
* `GET /api/operations/yard`: atendimentos não finalizados, priorizando `EM_LAVAGEM`, `AGUARDANDO_PATIO`, `PRONTO` e `AGENDADO`.
* `PATCH /api/service-orders/{id}/pause`: pausa uma lavagem atribuída ao lavador; exige motivo (`LUNCH`, `RESTROOM`, `OTHER`) e descrição para `OTHER`.
* `PATCH /api/service-orders/{id}/resume`: retoma a lavagem pausada pelo próprio lavador.
* `PATCH /api/service-orders/{id}/finalize`: recebe `paymentMethod`; em uma transação, muda o status para `FINALIZADO`, registra o pagamento pelo preço do catálogo e credita a comissão.
* `GET /api/operations/report?from=YYYY-MM-DD&to=YYYY-MM-DD`: relatório do gerente por data Manaus, com faturamento, comissões, serviços, pagamentos, pausas e tempo médio de lavagem (descontadas as pausas); intervalo máximo de 366 dias.
* `GET /api/users`: lista as contas para o gerente.
* `POST /api/users`: cria uma conta `LAVADOR` com `name`, `phone` e `password`.
* `PATCH /api/users/{id}/profile`: altera o perfil para `CUSTOMER`, `WASHER` ou `MANAGER`.
* `PATCH /api/users/{id}/active`: recebe `{"active": false}` para desativar ou `true` para reativar a conta.

O cliente mantém o painel aberto com atualização automática a cada 3 segundos. Quando o status é `PRONTO`, o painel mostra uma orientação persistente para retirada enquanto o atendimento não for finalizado. Não há envio de push com o app fechado.

### Bootstrap e limites de autenticação
* Em produção, configure `AUTH_TOKEN_SECRET` (mínimo 32 caracteres).
* O gerente inicial sem senha pode ser ativado uma única vez com `BOOTSTRAP_MANAGER_PHONE` e `BOOTSTRAP_MANAGER_PASSWORD`; após definir a senha, o login usa o hash BCrypt persistido.
* Contas preexistentes sem `senha_hash` não podem entrar em produção até que sua senha seja configurada por um processo administrativo seguro. Apenas ambiente de desenvolvimento permite reivindicar uma conta sem hash no primeiro login.
* A senha comprova acesso à conta após o cadastro, mas o telefone não é verificado por SMS/WhatsApp (OTP). Para comprovar posse do número, é necessária integração com provedor de OTP.
