package com.washflow.data.usecases.agendaratendimento;

import com.washflow.data.protocols.db.BuscarServicoPrecoPorIdRepository;
import com.washflow.data.protocols.db.BuscarVeiculoPorIdRepository;
import com.washflow.data.protocols.db.CriarAtendimentoRepository;
import com.washflow.domain.entities.Atendimento;
import com.washflow.domain.entities.StatusAtendimento;
import com.washflow.domain.errors.ServicoPrecoNaoEncontradoError;
import com.washflow.domain.errors.VeiculoNaoEncontradoError;
import com.washflow.domain.usecases.AgendarAtendimento;
import java.time.Instant;
import java.util.UUID;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete. Swapping the persistence library later (Hibernate, jOOQ, whatever) means writing new
 * {@code infra.db.*} implementations of those same ports; this class doesn't change.
 */
public class DbAgendarAtendimento implements AgendarAtendimento {

  private final BuscarVeiculoPorIdRepository buscarVeiculoPorIdRepository;
  private final BuscarServicoPrecoPorIdRepository buscarServicoPrecoPorIdRepository;
  private final CriarAtendimentoRepository criarAtendimentoRepository;

  public DbAgendarAtendimento(
      BuscarVeiculoPorIdRepository buscarVeiculoPorIdRepository,
      BuscarServicoPrecoPorIdRepository buscarServicoPrecoPorIdRepository,
      CriarAtendimentoRepository criarAtendimentoRepository) {
    this.buscarVeiculoPorIdRepository = buscarVeiculoPorIdRepository;
    this.buscarServicoPrecoPorIdRepository = buscarServicoPrecoPorIdRepository;
    this.criarAtendimentoRepository = criarAtendimentoRepository;
  }

  @Override
  public Atendimento agendar(Params params)
      throws VeiculoNaoEncontradoError, ServicoPrecoNaoEncontradoError {
    buscarVeiculoPorIdRepository
        .buscarPorId(params.veiculoId())
        .orElseThrow(() -> new VeiculoNaoEncontradoError(params.veiculoId()));

    buscarServicoPrecoPorIdRepository
        .buscarPorId(params.servicoPrecoId())
        .orElseThrow(() -> new ServicoPrecoNaoEncontradoError(params.servicoPrecoId()));

    Instant agora = Instant.now();
    Atendimento novoAtendimento =
        new Atendimento(
            UUID.randomUUID(),
            params.veiculoId(),
            params.servicoPrecoId(),
            StatusAtendimento.AGENDADO,
            params.dataAgendamento(),
            agora,
            agora);

    return criarAtendimentoRepository.criar(novoAtendimento);
  }
}
