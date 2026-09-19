-- Fixed ids so `POST /api/atendimentos` is curl-able right after `make db-up`,
-- without needing the (not-yet-built) usuarios/veiculos creation endpoints.

INSERT INTO usuarios (id, nome, telefone, perfil)
VALUES ('11111111-1111-1111-1111-111111111111', 'Cliente Demo', '11999990000', 'CLIENTE');

INSERT INTO veiculos (id, cliente_id, placa, modelo, cor)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'ABC1D23',
    'Onix',
    'Prata'
);

INSERT INTO servicos_preco (id, nome, preco, comissao_lavador)
VALUES (1, 'Lavagem Completa', 60.00, 20.00);
