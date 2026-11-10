CREATE TABLE atendimento_servicos (
    atendimento_id UUID NOT NULL REFERENCES atendimentos (id),
    servico_preco_id INT NOT NULL REFERENCES servicos_preco (id),
    ordem INT NOT NULL CHECK (ordem >= 0),
    CONSTRAINT pk_atendimento_servicos PRIMARY KEY (atendimento_id, servico_preco_id),
    CONSTRAINT uq_atendimento_servicos_ordem UNIQUE (atendimento_id, ordem)
);

INSERT INTO atendimento_servicos (atendimento_id, servico_preco_id, ordem)
SELECT id, servico_preco_id, 0
FROM atendimentos;
