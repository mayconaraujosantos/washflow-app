-- WhatsApp de cada lava-jato: avisos ao cliente e agendamento pela conversa.

-- Lembrete de uma hora antes do agendamento: marcado antes do envio (compare-and-set), para
-- que duas réplicas, ou duas rodadas da tarefa, nunca mandem o mesmo lembrete duas vezes.
ALTER TABLE atendimentos ADD COLUMN lembrete_enviado_em TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_atendimentos_lembrete_pendente
    ON atendimentos (data_agendamento)
    WHERE status = 'AGENDADO' AND lembrete_enviado_em IS NULL;

-- Em que passo está a conversa de cada telefone com o assistente e o que já foi escolhido
-- (JSON). Uma por telefone em cada lava-jato; conversas paradas recomeçam do menu.
CREATE TABLE conversas_whatsapp (
    estabelecimento_id UUID NOT NULL REFERENCES estabelecimentos (id),
    telefone VARCHAR(20) NOT NULL,
    etapa VARCHAR(30) NOT NULL,
    dados TEXT NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (estabelecimento_id, telefone)
);

-- Mensagens já tratadas: o provedor reenvia o webhook quando não recebe resposta a tempo, e
-- uma mensagem repetida nunca pode agendar duas vezes. Apagadas depois de alguns dias.
CREATE TABLE whatsapp_mensagens_recebidas (
    estabelecimento_id UUID NOT NULL REFERENCES estabelecimentos (id),
    mensagem_id VARCHAR(128) NOT NULL,
    recebida_em TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (estabelecimento_id, mensagem_id)
);
CREATE INDEX idx_whatsapp_mensagens_recebidas_data ON whatsapp_mensagens_recebidas (recebida_em);
