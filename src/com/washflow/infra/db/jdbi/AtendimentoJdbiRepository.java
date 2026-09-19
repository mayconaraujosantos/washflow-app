package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CriarAtendimentoRepository;
import com.washflow.domain.entities.Atendimento;
import org.jdbi.v3.core.Jdbi;

public class AtendimentoJdbiRepository implements CriarAtendimentoRepository {

  private final Jdbi jdbi;

  public AtendimentoJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Atendimento criar(Atendimento atendimento) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO atendimentos
                      (id, veiculo_id, servico_preco_id, status, data_agendamento, criado_em, atualizado_em)
                    VALUES
                      (:id, :veiculoId, :servicoPrecoId, :status, :dataAgendamento, :criadoEm, :atualizadoEm)
                    """)
                .bind("id", atendimento.id())
                .bind("veiculoId", atendimento.veiculoId())
                .bind("servicoPrecoId", atendimento.servicoPrecoId())
                .bind("status", atendimento.status().name())
                .bind("dataAgendamento", atendimento.dataAgendamento())
                .bind("criadoEm", atendimento.criadoEm())
                .bind("atualizadoEm", atendimento.atualizadoEm())
                .execute());

    return atendimento;
  }
}
