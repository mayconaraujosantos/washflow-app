-- A second fixed-id demo user with perfil=LAVADOR, so the new check-in/start/
-- complete/finalize endpoints are curl-able right after `make db-up`, same
-- reasoning as V2's demo cliente/veiculo.

INSERT INTO usuarios (id, nome, telefone, perfil)
VALUES ('33333333-3333-3333-3333-333333333333', 'Lavador Demo', '11999991111', 'LAVADOR');
