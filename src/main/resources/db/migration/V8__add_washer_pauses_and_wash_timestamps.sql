ALTER TABLE atendimentos ADD COLUMN lavagem_iniciada_em TIMESTAMPTZ;
ALTER TABLE atendimentos ADD COLUMN lavagem_concluida_em TIMESTAMPTZ;

CREATE TABLE pausas_atendimento (
    id UUID PRIMARY KEY,
    atendimento_id UUID NOT NULL REFERENCES atendimentos (id),
    lavador_id UUID NOT NULL REFERENCES usuarios (id),
    motivo VARCHAR(20) NOT NULL CHECK (motivo IN ('LUNCH', 'RESTROOM', 'OTHER')),
    descricao VARCHAR(200),
    iniciada_em TIMESTAMPTZ NOT NULL,
    retomada_em TIMESTAMPTZ
);

CREATE INDEX idx_pausas_atendimento_order ON pausas_atendimento (atendimento_id);
