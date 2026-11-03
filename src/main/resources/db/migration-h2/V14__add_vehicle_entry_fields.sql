-- H2 equivalent of ../migration/V14__add_vehicle_entry_fields.sql (TIMESTAMPTZ -> TIMESTAMP WITH TIME ZONE).
-- Entrada de veículo registrada pelo gerente no balcão (walk-in) ou agendada por ele.
-- previsao_entrega / observacoes: combinados com o cliente na entrada.
-- contato_nome / contato_telefone: dono do veículo sem cadastro completo
-- (veiculos.cliente_id continua nulo nesses casos, ver CLAUDE.md 2.2).

ALTER TABLE atendimentos ADD COLUMN previsao_entrega TIMESTAMP WITH TIME ZONE;
ALTER TABLE atendimentos ADD COLUMN observacoes VARCHAR(500);

ALTER TABLE veiculos ADD COLUMN contato_nome VARCHAR(100);
ALTER TABLE veiculos ADD COLUMN contato_telefone VARCHAR(20);

-- Agenda do gerente: atendimentos por período de data_agendamento.
CREATE INDEX idx_atendimentos_data_agendamento ON atendimentos (data_agendamento);
