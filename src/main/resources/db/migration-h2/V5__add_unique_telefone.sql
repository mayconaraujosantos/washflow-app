-- H2 equivalent of ../migration/V5__add_unique_telefone.sql - no dialect
-- differences here, kept identical for consistency with V2/V4.

ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_telefone UNIQUE (telefone);
