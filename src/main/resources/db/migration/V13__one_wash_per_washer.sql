-- Um lavador lava um carro por vez: no máximo um atendimento EM_LAVAGEM ou PAUSADA por lavador.
-- O DbStartServiceOrder já confere antes de iniciar, mas dois "Iniciar" do mesmo lavador em
-- carros diferentes, no mesmo instante, passariam os dois pela conferência; o índice faz o
-- segundo UPDATE falhar (vira 409 WASHER_BUSY), como ux_atendimentos_veiculo_no_patio faz com
-- o mesmo carro entrando duas vezes no pátio.
--
-- Se esta migração falhar com "could not create unique index", algum lavador já tem duas
-- lavagens abertas: conclua (PRONTO) uma delas e reinicie o app.
CREATE UNIQUE INDEX ux_atendimentos_lavador_em_andamento
  ON atendimentos (lavador_id)
  WHERE status IN ('EM_LAVAGEM', 'PAUSADA');
