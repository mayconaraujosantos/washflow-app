-- Código de acesso do cliente também por e-mail: cada código vai para um telefone (WhatsApp)
-- ou para um e-mail, nunca os dois.
ALTER TABLE codigos_acesso ALTER COLUMN telefone DROP NOT NULL;
ALTER TABLE codigos_acesso ADD COLUMN email VARCHAR(254);
ALTER TABLE codigos_acesso
    ADD CONSTRAINT ck_codigos_acesso_destino CHECK ((telefone IS NULL) <> (email IS NULL));
CREATE INDEX idx_codigos_acesso_email ON codigos_acesso (estabelecimento_id, email);

-- O e-mail da equipe continua único na plataforma (o login do gerente diz o lava-jato);
-- o do cliente é único dentro do lava-jato, como telefone e conta Google.
ALTER TABLE usuarios DROP CONSTRAINT uq_usuarios_email;
CREATE UNIQUE INDEX ux_usuarios_email_equipe ON usuarios (email) WHERE perfil <> 'CLIENTE';
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_email UNIQUE (estabelecimento_id, email);
