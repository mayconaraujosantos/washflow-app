-- Checklists de entrada e saída para ordens de serviço
-- Permite registrar o estado do veículo com itens de checklist e assinatura digital

CREATE TABLE service_order_checklist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    atendimento_id UUID NOT NULL REFERENCES atendimentos (id),
    tipo VARCHAR(6) NOT NULL CHECK (tipo IN ('ENTRY', 'EXIT')),
    itens JSONB NOT NULL DEFAULT '[]'::jsonb,
    assinatura_imagem TEXT,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    criado_por UUID REFERENCES usuarios (id),
    UNIQUE (atendimento_id, tipo)
);

CREATE INDEX idx_service_order_checklist_atendimento_id ON service_order_checklist (atendimento_id);
