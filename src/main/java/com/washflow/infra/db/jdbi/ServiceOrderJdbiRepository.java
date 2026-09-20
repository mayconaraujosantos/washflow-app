package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CreateServiceOrderRepository;
import com.washflow.domain.entities.ServiceOrder;
import org.jdbi.v3.core.Jdbi;

public class ServiceOrderJdbiRepository implements CreateServiceOrderRepository {

  private static final String INSERT_SQL =
      """
      INSERT INTO atendimentos
        (id, veiculo_id, servico_preco_id, status, data_agendamento, criado_em, atualizado_em)
      VALUES
        (:id, :vehicleId, :servicePriceId, :status, :scheduledAt, :createdAt, :updatedAt)
      """;

  private final Jdbi jdbi;

  public ServiceOrderJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public ServiceOrder create(ServiceOrder serviceOrder) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(INSERT_SQL)
                .bind("id", serviceOrder.id())
                .bind("vehicleId", serviceOrder.vehicleId())
                .bind("servicePriceId", serviceOrder.servicePriceId())
                .bind("status", serviceOrder.status().dbValue())
                .bind("scheduledAt", serviceOrder.scheduledAt())
                .bind("createdAt", serviceOrder.createdAt())
                .bind("updatedAt", serviceOrder.updatedAt())
                .execute());

    return serviceOrder;
  }
}
