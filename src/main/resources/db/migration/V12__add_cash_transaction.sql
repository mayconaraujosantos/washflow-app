-- Controle de caixa e despesas
-- Permite registrar entradas e saídas financeiras com categorias

CREATE TABLE expense_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(100) NOT NULL,
    cor VARCHAR(7) NOT NULL DEFAULT '#6B7280',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Categorias padrão
INSERT INTO expense_category (nome, cor) VALUES
    ('Produtos', '#EF4444'),
    ('Manutenção', '#F59E0B'),
    ('Aluguel', '#3B82F6'),
    ('Energia', '#10B981'),
    ('Água', '#06B6D4'),
    ('Salários', '#8B5CF6'),
    ('Outros', '#6B7280');

CREATE TABLE cash_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(8) NOT NULL CHECK (tipo IN ('INCOME', 'EXPENSE')),
    valor DECIMAL(10, 2) NOT NULL,
    categoria_id UUID REFERENCES expense_category (id),
    descricao TEXT,
    data_referencia DATE NOT NULL,
    atendimento_id UUID REFERENCES atendimentos (id),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    criado_por UUID REFERENCES usuarios (id)
);

CREATE INDEX idx_cash_transaction_data_referencia ON cash_transaction (data_referencia);
CREATE INDEX idx_cash_transaction_tipo ON cash_transaction (tipo);
CREATE INDEX idx_cash_transaction_categoria_id ON cash_transaction (categoria_id);
