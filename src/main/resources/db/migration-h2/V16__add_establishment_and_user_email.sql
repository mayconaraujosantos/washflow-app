-- H2 equivalent of ../migration/V16__add_establishment_and_user_email.sql.
-- Estabelecimento (instalação de um único lava-jato): parâmetros de funcionamento
-- e do pátio editáveis pelo gerente, e o momento em que o assistente de
-- configuração foi concluído. Linha única (id = 1).
CREATE TABLE estabelecimento (
    id INT PRIMARY KEY CHECK (id = 1),
    nome VARCHAR(100) NOT NULL,
    hora_abertura INT NOT NULL DEFAULT 8 CHECK (hora_abertura BETWEEN 0 AND 23),
    hora_fechamento INT NOT NULL DEFAULT 18 CHECK (hora_fechamento BETWEEN 1 AND 24),
    abre_domingo BOOLEAN NOT NULL DEFAULT FALSE,
    max_veiculos_hora INT NOT NULL DEFAULT 3 CHECK (max_veiculos_hora > 0),
    configurado_em TIMESTAMP WITH TIME ZONE,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CHECK (hora_abertura < hora_fechamento)
);

INSERT INTO estabelecimento (id, nome) VALUES (1, 'Meu Lava-Jato');

-- Login da gestão por e-mail; guardado em minúsculas pela aplicação.
ALTER TABLE usuarios ADD COLUMN email VARCHAR(254);
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_email UNIQUE (email);
