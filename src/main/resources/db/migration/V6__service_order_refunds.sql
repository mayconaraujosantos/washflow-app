-- Estorno de atendimento finalizado (ver CLAUDE.md). O pagamento original nunca é alterado: o
-- estorno é um lançamento novo, na data em que foi feito, e o atendimento passa a ESTORNADO.
-- Um único estorno (total) por atendimento; a comissão do lavador só é estornada se o gerente
-- escolher (comissao_estornada > 0).
CREATE TABLE estornos_atendimento (
  id UUID PRIMARY KEY,
  estabelecimento_id UUID NOT NULL REFERENCES estabelecimentos (id),
  atendimento_id UUID NOT NULL UNIQUE REFERENCES atendimentos (id),
  valor DECIMAL(10, 2) NOT NULL CHECK (valor >= 0),
  forma_devolucao VARCHAR(20) NOT NULL
    CHECK (forma_devolucao IN ('CASH', 'PIX', 'DEBIT_CARD', 'CREDIT_CARD')),
  motivo VARCHAR(20) NOT NULL
    CHECK (motivo IN ('CUSTOMER_COMPLAINT', 'WRONG_CHARGE', 'SERVICE_NOT_DONE', 'OTHER')),
  descricao VARCHAR(200),
  comissao_estornada DECIMAL(10, 2) DEFAULT 0 NOT NULL CHECK (comissao_estornada >= 0),
  criado_por UUID NOT NULL REFERENCES usuarios (id),
  criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_estornos_atendimento_estabelecimento_criado_em
  ON estornos_atendimento (estabelecimento_id, criado_em);

-- Lançamento financeiro: somente inserção, como a auditoria.
CREATE FUNCTION estornos_somente_insercao() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'estornos_atendimento é somente inserção (% recusado)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_estornos_somente_insercao
  BEFORE UPDATE OR DELETE ON estornos_atendimento
  FOR EACH ROW EXECUTE FUNCTION estornos_somente_insercao();
