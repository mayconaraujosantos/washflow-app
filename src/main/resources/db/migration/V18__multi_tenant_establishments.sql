-- Multi-lava-jato: uma instalação atende vários estabelecimentos.
-- Decisão do dono: a plataforma começa limpa - os dados de teste da instalação
-- única ("Jorge") são descartados. Cada linha operacional passa a pertencer a um
-- estabelecimento (estabelecimento_id) e a unicidade de telefone, placa e conta
-- Google vale dentro do estabelecimento. O administrador da plataforma fica em
-- tabela própria, fora de qualquer estabelecimento.

DELETE FROM codigos_acesso;
DELETE FROM venda_pagamentos;
DELETE FROM venda_itens;
DELETE FROM vendas;
DELETE FROM cash_transaction;
DELETE FROM expense_category;
DELETE FROM service_order_photo;
DELETE FROM service_order_checklist;
DELETE FROM pausas_atendimento;
DELETE FROM comissoes_lavador;
DELETE FROM pagamentos;
DELETE FROM atendimentos;
DELETE FROM veiculos;
DELETE FROM servicos_preco;
DELETE FROM usuarios;
DROP TABLE estabelecimento;

CREATE TABLE estabelecimentos (
    id UUID PRIMARY KEY,
    slug VARCHAR(60) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    hora_abertura INT NOT NULL DEFAULT 8 CHECK (hora_abertura BETWEEN 0 AND 23),
    hora_fechamento INT NOT NULL DEFAULT 18 CHECK (hora_fechamento BETWEEN 1 AND 24),
    abre_domingo BOOLEAN NOT NULL DEFAULT FALSE,
    max_veiculos_hora INT NOT NULL DEFAULT 3 CHECK (max_veiculos_hora > 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_estabelecimentos_slug UNIQUE (slug),
    CHECK (hora_abertura < hora_fechamento)
);

CREATE TABLE administradores (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_administradores_email UNIQUE (email)
);

ALTER TABLE usuarios ADD COLUMN estabelecimento_id UUID NOT NULL;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_estabelecimento
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos (id);
ALTER TABLE usuarios DROP CONSTRAINT uq_usuarios_telefone;
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_telefone UNIQUE (estabelecimento_id, telefone);
ALTER TABLE usuarios DROP CONSTRAINT uq_usuarios_google;
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_google UNIQUE (estabelecimento_id, google_sub);

ALTER TABLE veiculos ADD COLUMN estabelecimento_id UUID NOT NULL;
ALTER TABLE veiculos ADD CONSTRAINT fk_veiculos_estabelecimento
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos (id);
ALTER TABLE veiculos DROP CONSTRAINT veiculos_placa_key;
ALTER TABLE veiculos ADD CONSTRAINT uq_veiculos_placa UNIQUE (estabelecimento_id, placa);

ALTER TABLE servicos_preco ADD COLUMN estabelecimento_id UUID NOT NULL;
ALTER TABLE servicos_preco ADD CONSTRAINT fk_servicos_preco_estabelecimento
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos (id);
CREATE INDEX idx_servicos_preco_estabelecimento ON servicos_preco (estabelecimento_id);

ALTER TABLE atendimentos ADD COLUMN estabelecimento_id UUID NOT NULL;
ALTER TABLE atendimentos ADD CONSTRAINT fk_atendimentos_estabelecimento
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos (id);
CREATE INDEX idx_atendimentos_estabelecimento_status
    ON atendimentos (estabelecimento_id, status, atualizado_em);
CREATE INDEX idx_atendimentos_estabelecimento_agenda
    ON atendimentos (estabelecimento_id, data_agendamento);

ALTER TABLE vendas ADD COLUMN estabelecimento_id UUID NOT NULL;
ALTER TABLE vendas ADD CONSTRAINT fk_vendas_estabelecimento
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos (id);
CREATE INDEX idx_vendas_estabelecimento_criado_em ON vendas (estabelecimento_id, criado_em);

ALTER TABLE codigos_acesso ADD COLUMN estabelecimento_id UUID NOT NULL;
ALTER TABLE codigos_acesso ADD CONSTRAINT fk_codigos_acesso_estabelecimento
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos (id);

ALTER TABLE expense_category ADD COLUMN estabelecimento_id UUID NOT NULL;
ALTER TABLE expense_category ADD CONSTRAINT fk_expense_category_estabelecimento
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos (id);

ALTER TABLE cash_transaction ADD COLUMN estabelecimento_id UUID NOT NULL;
ALTER TABLE cash_transaction ADD CONSTRAINT fk_cash_transaction_estabelecimento
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos (id);
CREATE INDEX idx_cash_transaction_estabelecimento_data
    ON cash_transaction (estabelecimento_id, data_referencia);
