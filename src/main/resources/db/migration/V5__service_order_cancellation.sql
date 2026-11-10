-- Cancelamento de atendimento antes da lavagem (cliente desistiu, não compareceu, lançado por
-- engano). O atendimento fica com status CANCELADO e não é apagado: guarda quem cancelou, quando
-- e por quê. CANCELADO não ocupa vaga da hora nem o pátio (ux_atendimentos_veiculo_no_patio).
ALTER TABLE atendimentos
  ADD COLUMN cancelado_em TIMESTAMP WITH TIME ZONE,
  ADD COLUMN cancelado_por UUID REFERENCES usuarios (id),
  ADD COLUMN motivo_cancelamento VARCHAR(20)
    CHECK (motivo_cancelamento IN ('CUSTOMER_GAVE_UP', 'NO_SHOW', 'REGISTERED_BY_MISTAKE', 'OTHER')),
  ADD COLUMN descricao_cancelamento VARCHAR(200);
