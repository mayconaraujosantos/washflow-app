package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Atendimento;

public interface CriarAtendimentoRepository {

  Atendimento criar(Atendimento atendimento);
}
