-- Schema from CLAUDE.md's domain dictionary. Postgres 13+ has gen_random_uuid()
-- built into core, so no extension is needed for the ids we generate in SQL
-- (application-generated ids, like atendimentos.id, are inserted explicitly
-- instead - id generation for that entity belongs to the domain layer).

CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(100) NOT NULL,
    telefone VARCHAR(20),
    senha_hash VARCHAR(255),
    perfil VARCHAR(20) NOT NULL CHECK (perfil IN ('CLIENTE', 'LAVADOR', 'GERENTE')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE veiculos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID REFERENCES usuarios (id),
    placa VARCHAR(10) NOT NULL UNIQUE,
    modelo VARCHAR(50),
    cor VARCHAR(30)
);

CREATE TABLE servicos_preco (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    preco DECIMAL(10, 2) NOT NULL,
    comissao_lavador DECIMAL(10, 2)
);

CREATE TABLE atendimentos (
    id UUID PRIMARY KEY,
    veiculo_id UUID NOT NULL REFERENCES veiculos (id),
    servico_preco_id INT NOT NULL REFERENCES servicos_preco (id),
    lavador_id UUID REFERENCES usuarios (id),
    status VARCHAR(25) NOT NULL DEFAULT 'AGENDADO',
    data_agendamento TIMESTAMPTZ NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Backs the "fila de espera" query from CLAUDE.md: filter by status, order by
-- atualizado_em.
CREATE INDEX idx_atendimentos_status_atualizado_em ON atendimentos (status, atualizado_em);
