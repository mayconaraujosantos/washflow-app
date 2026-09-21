package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CreateWasherCommissionRepository;
import com.washflow.domain.entities.WasherCommission;
import org.jdbi.v3.core.Jdbi;

public class WasherCommissionJdbiRepository implements CreateWasherCommissionRepository {

  private static final String INSERT_SQL =
      """
      INSERT INTO comissoes_lavador
        (id, atendimento_id, lavador_id, valor, criado_em)
      VALUES
        (:id, :serviceOrderId, :washerId, :amount, :creditedAt)
      """;

  private final Jdbi jdbi;

  public WasherCommissionJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public WasherCommission create(WasherCommission commission) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(INSERT_SQL)
                .bind("id", commission.id())
                .bind("serviceOrderId", commission.serviceOrderId())
                .bind("washerId", commission.washerId())
                .bind("amount", commission.amount())
                .bind("creditedAt", commission.creditedAt())
                .execute());

    return commission;
  }
}
