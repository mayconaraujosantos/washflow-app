-- H2 equivalent of ../migration/V17__add_badges_tickets_and_customer_otp.sql.
-- Acesso por perfil: crachá/PIN do lavador, ticket público do atendimento,
-- código de acesso (OTP) por WhatsApp e login Google do cliente.

-- Crachá: PIN (BCrypt) e token do QR (HMAC, para busca) emitidos pelo gerente.
ALTER TABLE usuarios ADD COLUMN pin_hash VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN cracha_hash VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN pin_tentativas INT NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN pin_bloqueado_ate TIMESTAMP WITH TIME ZONE;
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_cracha UNIQUE (cracha_hash);

-- Login Google: "sub" estável da conta Google.
ALTER TABLE usuarios ADD COLUMN google_sub VARCHAR(255);
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_google UNIQUE (google_sub);

-- Ticket: token aleatório entregue ao cliente (QR) para acompanhar sem login.
ALTER TABLE atendimentos ADD COLUMN ticket_token VARCHAR(64);
ALTER TABLE atendimentos ADD CONSTRAINT uq_atendimentos_ticket UNIQUE (ticket_token);

-- Códigos de acesso por WhatsApp (um ativo por telefone).
CREATE TABLE codigos_acesso (
    id UUID PRIMARY KEY,
    telefone VARCHAR(20) NOT NULL,
    codigo_hash VARCHAR(64) NOT NULL,
    tentativas INT NOT NULL DEFAULT 0,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_codigos_acesso_telefone ON codigos_acesso (telefone);
