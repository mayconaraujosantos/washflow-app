CREATE TABLE pagamentos (
    id UUID PRIMARY KEY,
    atendimento_id UUID NOT NULL UNIQUE REFERENCES atendimentos (id),
    forma_pagamento VARCHAR(20) NOT NULL CHECK (
        forma_pagamento IN ('CASH', 'PIX', 'DEBIT_CARD', 'CREDIT_CARD')
    ),
    valor DECIMAL(10, 2) NOT NULL,
    recebido_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
