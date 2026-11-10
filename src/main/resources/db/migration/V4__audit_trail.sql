-- Trilha de auditoria (ver CLAUDE.md, "Auditoria"): toda alteração autenticada bem-sucedida
-- (POST, PUT, PATCH, DELETE com resposta 2xx) - quem fez, quando, em qual rota e com que dados
-- (senhas, PINs, tokens, códigos e imagens mascarados). Serve para disputas de caixa, então é
-- somente inserção: UPDATE e DELETE são recusados pelo próprio banco.
CREATE TABLE auditoria (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  -- NULL para ações do administrador da plataforma, que não pertence a nenhum lava-jato.
  estabelecimento_id UUID REFERENCES estabelecimentos (id),
  -- Sem chave estrangeira: pode ser um usuário ou o administrador da plataforma.
  usuario_id UUID NOT NULL,
  perfil VARCHAR(20) NOT NULL,
  metodo VARCHAR(10) NOT NULL,
  rota VARCHAR(200) NOT NULL,
  caminho VARCHAR(300) NOT NULL,
  status INT NOT NULL,
  detalhes TEXT NOT NULL DEFAULT '',
  criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_auditoria_estabelecimento_criado_em ON auditoria (estabelecimento_id, criado_em);

CREATE FUNCTION auditoria_somente_insercao() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'auditoria é somente inserção (% recusado)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_auditoria_somente_insercao
  BEFORE UPDATE OR DELETE ON auditoria
  FOR EACH ROW EXECUTE FUNCTION auditoria_somente_insercao();
