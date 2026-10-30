-- H2 (MODE=PostgreSQL) equivalent of ../migration/V1__init.sql, used only when
-- JdbiFactory falls back to the in-memory H2 database because Postgres is
-- unreachable at startup. Keep this in sync with the Postgres migration -
-- H2 just needs gen_random_uuid() -> RANDOM_UUID() (and reordered after the
-- column type, since H2 doesn't accept PRIMARY KEY before DEFAULT) and
-- TIMESTAMPTZ -> TIMESTAMP WITH TIME ZONE (H2 doesn't recognize the
-- Postgres-only alias).

CREATE TABLE usuarios (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    telefone VARCHAR(20),
    senha_hash VARCHAR(255),
    perfil VARCHAR(20) NOT NULL CHECK (perfil IN ('CLIENTE', 'LAVADOR', 'GERENTE')),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE veiculos (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
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
    data_agendamento TIMESTAMP WITH TIME ZONE NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Backs the "fila de espera" query from CLAUDE.md: filter by status, order by
-- atualizado_em.
CREATE INDEX idx_atendimentos_status_atualizado_em ON atendimentos (status, atualizado_em);
