ALTER TABLE atendimentos ADD COLUMN lavagem_iniciada_em TIMESTAMP WITH TIME ZONE;
ALTER TABLE atendimentos ADD COLUMN lavagem_concluida_em TIMESTAMP WITH TIME ZONE;

CREATE TABLE pausas_atendimento (
    id UUID PRIMARY KEY,
    atendimento_id UUID NOT NULL REFERENCES atendimentos (id),
    lavador_id UUID NOT NULL REFERENCES usuarios (id),
    motivo VARCHAR(20) NOT NULL CHECK (motivo IN ('LUNCH', 'RESTROOM', 'OTHER')),
    descricao VARCHAR(200),
    iniciada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    retomada_em TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_pausas_atendimento_order ON pausas_atendimento (atendimento_id);
