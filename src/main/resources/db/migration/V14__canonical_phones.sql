-- Um telefone, uma grafia: só dígitos, sem o +55, e celular sempre com o nono dígito
-- (ContactRules.normalizePhone). Antes o cadastro de lavador e o login gravavam o que vinha
-- ("559286061046"), e a regra uq_usuarios_telefone não via que era o mesmo número de um
-- cliente ("92986061046").
--
-- Contas: só mudam quando a nova grafia não é de outra conta do mesmo lava-jato. Se for, as
-- duas contas são da mesma pessoa e ficam como estão para alguém decidir qual manter (o aviso
-- abaixo aparece no log da migração). Contatos do balcão, vendas e códigos não têm unicidade.

CREATE FUNCTION pg_temp.telefone_canonico(bruto TEXT) RETURNS TEXT
LANGUAGE SQL IMMUTABLE AS $$
  SELECT CASE
           WHEN length(d) = 10 AND substr(d, 3, 1) >= '6'
             THEN substr(d, 1, 2) || '9' || substr(d, 3)
           ELSE d
         END
  FROM (
    SELECT CASE
             WHEN digitos LIKE '55%' AND length(digitos) > 11 THEN substr(digitos, 3)
             ELSE digitos
           END AS d
    FROM (SELECT regexp_replace(bruto, '\D', '', 'g') AS digitos) AS s1
  ) AS s2
$$;

WITH canonico AS (
  SELECT id, estabelecimento_id, telefone, pg_temp.telefone_canonico(telefone) AS novo
  FROM usuarios
  WHERE telefone IS NOT NULL
)
UPDATE usuarios u
SET telefone = c.novo
FROM canonico c
WHERE u.id = c.id
  AND c.novo <> c.telefone
  -- nenhuma outra conta do lava-jato já tem (ou passaria a ter) essa grafia
  AND NOT EXISTS (
    SELECT 1 FROM canonico o
    WHERE o.estabelecimento_id = c.estabelecimento_id
      AND o.id <> c.id
      AND (o.telefone = c.novo OR o.novo = c.novo)
  );

DO $$
DECLARE
  conflito RECORD;
BEGIN
  FOR conflito IN
    SELECT u.estabelecimento_id, u.id, u.nome, u.perfil, u.telefone
    FROM usuarios u
    WHERE u.telefone IS NOT NULL
      AND u.telefone <> pg_temp.telefone_canonico(u.telefone)
  LOOP
    RAISE NOTICE 'Telefone % (% %, conta %) é o mesmo número de outra conta do lava-jato %: '
      'decida qual conta manter.',
      conflito.telefone, conflito.perfil, conflito.nome, conflito.id, conflito.estabelecimento_id;
  END LOOP;
END $$;

UPDATE veiculos SET contato_telefone = pg_temp.telefone_canonico(contato_telefone)
WHERE contato_telefone IS NOT NULL
  AND contato_telefone <> pg_temp.telefone_canonico(contato_telefone);

UPDATE vendas SET cliente_telefone = pg_temp.telefone_canonico(cliente_telefone)
WHERE cliente_telefone IS NOT NULL
  AND cliente_telefone <> pg_temp.telefone_canonico(cliente_telefone);

UPDATE codigos_acesso SET telefone = pg_temp.telefone_canonico(telefone)
WHERE telefone IS NOT NULL AND telefone <> pg_temp.telefone_canonico(telefone);
