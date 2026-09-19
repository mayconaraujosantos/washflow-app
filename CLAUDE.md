# Domínio e Estrutura de Dados (Washflow)

Este documento define as entidades de negócio, os estados do fluxo do veículo (fila lógica) e a estrutura relacional do sistema para garantir portabilidade entre bancos de dados físicos (ex: PostgreSQL) e ambientes de teste *In-Memory* (ex: H2 / Repositórios em memória).

---

## 1. Máquina de Estados do Veículo (O Washflow)

O ciclo de vida de um atendimento é linear e controlado pelo campo `status`. A ordenação da fila do pátio é baseada estritamente no campo `atualizado_em` das tabelas de serviço.

[ AGENDADO ] ➔ [ AGUARDANDO_PATIO ] ➔ [ EM_LAVAGEM ] ➔ [ PRONTO ] ➔ [ FINALIZADO ]

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

---

## 3. Lógica das Consultas para as Filas (Portabilidade In-Memory)

Para garantir que a lógica funcione igual no banco físico ou em listas Java/Kotlin na memória, as listagens devem seguir estas regras:

### Fila de Espera dos Lavadores (Status: AGUARDANDO_PATIO)
* **Regra:** Listar todos os atendimentos onde `status = 'AGUARDANDO_PATIO'` ordenados por `atualizado_em ASC` (Primeiro que chega, primeiro que sai).

### Painel do Gerente (Pátio Geral)
* **Regra:** Listar atendimentos agrupados por status, priorizando `EM_LAVAGEM` e `AGUARDANDO_PATIO` para monitorar gargalos de tempo.
