-- H2 (MODE=PostgreSQL) equivalent of ../migration/V3__add_comissoes_lavador.sql -
-- see that file's comment. Only TIMESTAMPTZ -> TIMESTAMP WITH TIME ZONE differs,
-- same swap as V1.

CREATE TABLE comissoes_lavador (
    id UUID PRIMARY KEY,
    atendimento_id UUID NOT NULL REFERENCES atendimentos (id),
    lavador_id UUID NOT NULL REFERENCES usuarios (id),
    valor DECIMAL(10, 2) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_comissoes_lavador_lavador_id ON comissoes_lavador (lavador_id);
