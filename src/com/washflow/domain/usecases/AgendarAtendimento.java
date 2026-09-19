package com.washflow.domain.usecases;

import com.washflow.domain.entities.Atendimento;
import com.washflow.domain.errors.ServicoPrecoNaoEncontradoError;
import com.washflow.domain.errors.VeiculoNaoEncontradoError;
import java.time.Instant;
import java.util.UUID;

/**
 * Schedules a new {@code atendimento}, starting it at {@link
 * com.washflow.domain.entities.StatusAtendimento#AGENDADO}. Implemented by {@code
 * DbAgendarAtendimento} in the data layer.
 */
public interface AgendarAtendimento {

  Atendimento agendar(Params params)
      throws VeiculoNaoEncontradoError, ServicoPrecoNaoEncontradoError;

  record Params(UUID veiculoId, int servicoPrecoId, Instant dataAgendamento) {}
}
