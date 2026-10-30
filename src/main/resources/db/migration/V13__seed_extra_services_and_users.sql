-- Adiciona mais tipos de serviços de lavagem para enriquecer o catálogo
-- Usa ON CONFLICT DO NOTHING para ser idempotente em redeploys

INSERT INTO servicos_preco (id, nome, preco, comissao_lavador)
VALUES
    (2, 'Lavagem Simples', 35.00, 10.00),
    (3, 'Lavagem Premium', 80.00, 25.00),
    (4, 'Lavagem Interna', 25.00, 8.00),
    (5, 'Lavagem Externa', 30.00, 10.00),
    (6, 'Polimento', 120.00, 35.00),
    (7, 'Hidratacao', 150.00, 40.00),
    (8, 'Lavagem + Wax', 90.00, 30.00),
    (9, 'Lavagem Executiva', 100.00, 30.00),
    (10, 'Detalhamento Completo', 250.00, 60.00)
ON CONFLICT (id) DO NOTHING;

-- 8 clientes de facil acesso com senha "1" e telefones fakers
INSERT INTO usuarios (id, nome, telefone, senha_hash, perfil, criado_em, ativo)
VALUES
    ('a1111111-1111-1111-1111-111111111111', 'Cliente 1', '11980001001', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'CLIENTE', now(), true),
    ('a1111111-1111-1111-1111-111111111112', 'Cliente 2', '11980001002', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'CLIENTE', now(), true),
    ('a1111111-1111-1111-1111-111111111113', 'Cliente 3', '11980001003', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'CLIENTE', now(), true),
    ('a1111111-1111-1111-1111-111111111114', 'Cliente 4', '11980001004', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'CLIENTE', now(), true),
    ('a1111111-1111-1111-1111-111111111115', 'Cliente 5', '11980001005', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'CLIENTE', now(), true),
    ('a1111111-1111-1111-1111-111111111116', 'Cliente 6', '11980001006', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'CLIENTE', now(), true),
    ('a1111111-1111-1111-1111-111111111117', 'Cliente 7', '11980001007', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'CLIENTE', now(), true),
    ('a1111111-1111-1111-1111-111111111118', 'Cliente 8', '11980001008', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'CLIENTE', now(), true)
ON CONFLICT (id) DO NOTHING;

-- 8 gerentes de facil acesso com senha "1" e telefones fakers
INSERT INTO usuarios (id, nome, telefone, senha_hash, perfil, criado_em, ativo)
VALUES
    ('a2222222-2222-2222-2222-222222222221', 'Gerente 1', '11980002001', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'GERENTE', now(), true),
    ('a2222222-2222-2222-2222-222222222222', 'Gerente 2', '11980002002', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'GERENTE', now(), true),
    ('a2222222-2222-2222-2222-222222222223', 'Gerente 3', '11980002003', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'GERENTE', now(), true),
    ('a2222222-2222-2222-2222-222222222224', 'Gerente 4', '11980002004', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'GERENTE', now(), true),
    ('a2222222-2222-2222-2222-222222222225', 'Gerente 5', '11980002005', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'GERENTE', now(), true),
    ('a2222222-2222-2222-2222-222222222226', 'Gerente 6', '11980002006', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'GERENTE', now(), true),
    ('a2222222-2222-2222-2222-222222222227', 'Gerente 7', '11980002007', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'GERENTE', now(), true),
    ('a2222222-2222-2222-2222-222222222228', 'Gerente 8', '11980002008', '$2a$12$1DKhf5093OX1wvSgis0h4egmE3BAw3ZAK7ZrQHm/8OLzPbIOUpAxy', 'GERENTE', now(), true)
ON CONFLICT (id) DO NOTHING;
