-- Proteções contra requisições simultâneas e reenvios (ver CLAUDE.md, "Concorrência e idempotência").

-- Um carro só pode ter um atendimento aberto no pátio por vez: registrar a mesma placa de novo
-- (ou receber um agendamento do mesmo carro) enquanto ele ainda está no pátio é recusado (409).
-- Agendamentos futuros (AGENDADO) e atendimentos finalizados não contam.
CREATE UNIQUE INDEX ux_atendimentos_veiculo_no_patio
  ON atendimentos (veiculo_id)
  WHERE status IN ('AGUARDANDO_PATIO', 'EM_LAVAGEM', 'PAUSADA', 'PRONTO');

-- Respostas de requisições com o cabeçalho Idempotency-Key: o reenvio com a mesma chave devolve a
-- resposta gravada em vez de executar de novo (venda, lançamento de caixa, entrada, agendamento).
-- Guarda só respostas de sucesso, por 24 horas; status_resposta NULL = ainda em processamento.
CREATE TABLE chaves_idempotencia (
  usuario_id UUID NOT NULL,
  chave VARCHAR(100) NOT NULL,
  estabelecimento_id UUID REFERENCES estabelecimentos (id),
  metodo VARCHAR(10) NOT NULL,
  rota VARCHAR(200) NOT NULL,
  hash_requisicao VARCHAR(64) NOT NULL,
  status_resposta INT,
  corpo_resposta TEXT,
  criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (usuario_id, chave)
);

CREATE INDEX ix_chaves_idempotencia_criado_em ON chaves_idempotencia (criado_em);
