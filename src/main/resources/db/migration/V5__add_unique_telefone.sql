-- Backs AuthenticateUser: a phone number is the login identity and is bound
-- to exactly one profile (CLIENTE/LAVADOR/GERENTE) for its whole lifetime, so
-- two different usuarios rows can never share one. NULL telefones (clients
-- without a full signup, per V1's comment) stay unrestricted - Postgres's
-- UNIQUE treats NULL as distinct from every other NULL.

ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_telefone UNIQUE (telefone);
