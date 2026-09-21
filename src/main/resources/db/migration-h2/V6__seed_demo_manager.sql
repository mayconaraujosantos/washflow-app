-- H2 equivalent of ../migration/V6__seed_demo_manager.sql - no dialect
-- differences here, kept identical for consistency with V2/V4.

INSERT INTO usuarios (id, nome, telefone, perfil)
VALUES ('44444444-4444-4444-4444-444444444444', 'Gerente Demo', '11999993333', 'GERENTE');
