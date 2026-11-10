-- Estorno de venda rápida (ver CLAUDE.md): total ou parcial, um lançamento por forma de devolução.
-- Cada forma só devolve até o que entrou nela na venda; os pagamentos originais nunca mudam.
CREATE TABLE venda_estornos (
  id UUID PRIMARY KEY,
  estabelecimento_id UUID NOT NULL REFERENCES estabelecimentos (id),
  venda_id UUID NOT NULL REFERENCES vendas (id),
  forma_devolucao VARCHAR(20) NOT NULL
    CHECK (forma_devolucao IN ('CASH', 'PIX', 'DEBIT_CARD', 'CREDIT_CARD')),
  valor DECIMAL(10, 2) NOT NULL CHECK (valor > 0),
  motivo VARCHAR(20) NOT NULL
    CHECK (motivo IN ('CUSTOMER_COMPLAINT', 'WRONG_CHARGE', 'SERVICE_NOT_DONE', 'OTHER')),
  descricao VARCHAR(200),
  criado_por UUID NOT NULL REFERENCES usuarios (id),
  criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_venda_estornos_venda_id ON venda_estornos (venda_id);

-- Lançamento financeiro: somente inserção, como estornos_atendimento.
CREATE FUNCTION venda_estornos_somente_insercao() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'venda_estornos é somente inserção (% recusado)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_venda_estornos_somente_insercao
  BEFORE UPDATE OR DELETE ON venda_estornos
  FOR EACH ROW EXECUTE FUNCTION venda_estornos_somente_insercao();
