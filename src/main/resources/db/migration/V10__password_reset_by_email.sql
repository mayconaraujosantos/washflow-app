-- Password reset by e-mail code (customers and managers).
--
-- Customers keep one account per lava-jato, so their e-mail cannot live in
-- usuarios.email (unique on the whole platform: it is the managers' login).
-- email_recuperacao is only where reset codes go; it is not unique.
ALTER TABLE usuarios ADD COLUMN email_recuperacao VARCHAR(254);

-- One pending code per account (a new request replaces it); only its keyed
-- hash is stored. Belongs to the lava-jato through the account.
CREATE TABLE codigos_senha (
    usuario_id UUID PRIMARY KEY REFERENCES usuarios (id),
    codigo_hash VARCHAR(64) NOT NULL,
    tentativas INT DEFAULT 0 NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);
