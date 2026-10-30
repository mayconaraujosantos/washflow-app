-- Fotos nas ordens de serviço
-- Permite registrar fotos do veículo na entrada e saída do serviço

CREATE TABLE service_order_photo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    atendimento_id UUID NOT NULL REFERENCES atendimentos (id) ON DELETE CASCADE,
    tipo VARCHAR(6) NOT NULL CHECK (tipo IN ('ENTRY', 'EXIT')),
    imagem_data TEXT NOT NULL,
    descricao TEXT,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    criado_por UUID REFERENCES usuarios (id)
);

CREATE INDEX idx_service_order_photo_atendimento_id ON service_order_photo (atendimento_id);
