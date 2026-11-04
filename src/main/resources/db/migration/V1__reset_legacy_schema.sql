-- Migrações unificadas (1.5.0): o mesmo conjunto roda no PostgreSQL e no H2.
--
-- Até a 1.4.x havia duas trilhas (db/migration e db/migration-h2) com o
-- histórico em flyway_schema_history. A plataforma multi-lava-jato começa limpa
-- (decisão do dono), então o histórico foi consolidado numa base única e passou
-- para a tabela schema_history. Num banco que ainda tem o schema antigo, esta
-- migração remove esse schema uma única vez (os dados eram de teste); num banco
-- novo, não faz nada.

DROP TABLE IF EXISTS codigos_acesso CASCADE;
DROP TABLE IF EXISTS venda_pagamentos CASCADE;
DROP TABLE IF EXISTS venda_itens CASCADE;
DROP TABLE IF EXISTS vendas CASCADE;
DROP TABLE IF EXISTS cash_transaction CASCADE;
DROP TABLE IF EXISTS expense_category CASCADE;
DROP TABLE IF EXISTS service_order_photo CASCADE;
DROP TABLE IF EXISTS service_order_checklist CASCADE;
DROP TABLE IF EXISTS pausas_atendimento CASCADE;
DROP TABLE IF EXISTS comissoes_lavador CASCADE;
DROP TABLE IF EXISTS pagamentos CASCADE;
DROP TABLE IF EXISTS atendimentos CASCADE;
DROP TABLE IF EXISTS veiculos CASCADE;
DROP TABLE IF EXISTS servicos_preco CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;
DROP TABLE IF EXISTS estabelecimento CASCADE;
DROP TABLE IF EXISTS estabelecimentos CASCADE;
DROP TABLE IF EXISTS administradores CASCADE;
DROP TABLE IF EXISTS flyway_schema_history CASCADE;
