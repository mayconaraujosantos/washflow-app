-- Finds audit rows written with a secret in clear text.
--
-- From 920ca3d until 1d5d81b, AuditTrail masked only keys named exactly
-- "password", "pin", "token"... so nested keys such as "badgeToken" or
-- "imageData" were stored as typed. Run against any database that ran a
-- build from that window (Render included):
--
--   psql "$DATABASE_URL" -f scripts/audit-unmasked-check.sql
--
-- Read-only. Only authenticated changes are audited, so in practice this
-- catches checklist photos and signatures (imageData...) sent by managers.
-- `auditoria` refuses UPDATE/DELETE (trigger): cleaning it is a deliberate
-- manual step.

\echo '== rows with an unmasked secret, by route'
SELECT rota, count(*) AS linhas, min(criado_em) AS primeira, max(criado_em) AS ultima
FROM auditoria
WHERE detalhes ~* '"[a-z_]*(password|senha|pin|token|secret|code|codigo|credential|image|photo|foto|signature|assinatura)[a-z_]*" *: *"[^*"]'
GROUP BY rota
ORDER BY linhas DESC;

