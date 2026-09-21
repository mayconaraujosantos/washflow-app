-- Backs DbFinalizeServiceOrder's washer-commission credit. Not yet in
-- CLAUDE.md's domain dictionary (see WasherCommission.java's Javadoc) - the
-- manager's cash-out/commission history flow is new and needs somewhere to
-- land its rows.

CREATE TABLE comissoes_lavador (
    id UUID PRIMARY KEY,
    atendimento_id UUID NOT NULL REFERENCES atendimentos (id),
    lavador_id UUID NOT NULL REFERENCES usuarios (id),
    valor DECIMAL(10, 2) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comissoes_lavador_lavador_id ON comissoes_lavador (lavador_id);
