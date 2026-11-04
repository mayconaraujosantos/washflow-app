-- H2 equivalent of ../migration/V15__add_sales_and_budgets.sql.
-- Venda rápida (balcão, sem passar pelo pátio) e orçamento.
-- Itens vêm do catálogo servicos_preco; preço e nome ficam congelados no item.
-- Pagamentos podem ser parciais e em mais de uma forma (venda_pagamentos).

CREATE TABLE vendas (
    id UUID PRIMARY KEY,
    tipo VARCHAR(12) NOT NULL CHECK (tipo IN ('VENDA', 'ORCAMENTO')),
    veiculo_id UUID REFERENCES veiculos (id),
    cliente_nome VARCHAR(100),
    cliente_telefone VARCHAR(20),
    subtotal DECIMAL(10, 2) NOT NULL,
    desconto DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total DECIMAL(10, 2) NOT NULL,
    observacoes VARCHAR(500),
    criado_por UUID REFERENCES usuarios (id),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_vendas_criado_em ON vendas (criado_em);

CREATE TABLE venda_itens (
    id UUID PRIMARY KEY,
    venda_id UUID NOT NULL REFERENCES vendas (id) ON DELETE CASCADE,
    servico_preco_id INT NOT NULL REFERENCES servicos_preco (id),
    descricao VARCHAR(50) NOT NULL,
    quantidade INT NOT NULL CHECK (quantidade > 0),
    preco_unitario DECIMAL(10, 2) NOT NULL,
    total DECIMAL(10, 2) NOT NULL
);

CREATE INDEX idx_venda_itens_venda_id ON venda_itens (venda_id);

CREATE TABLE venda_pagamentos (
    id UUID PRIMARY KEY,
    venda_id UUID NOT NULL REFERENCES vendas (id) ON DELETE CASCADE,
    forma_pagamento VARCHAR(20) NOT NULL
        CHECK (forma_pagamento IN ('CASH', 'PIX', 'DEBIT_CARD', 'CREDIT_CARD')),
    valor DECIMAL(10, 2) NOT NULL CHECK (valor > 0),
    recebido_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_venda_pagamentos_venda_id ON venda_pagamentos (venda_id);

-- Seeds inserted explicit ids into the identity column; move it past them.
ALTER TABLE servicos_preco ALTER COLUMN id RESTART WITH 1000;
