package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.ServicePayment;
import com.washflow.domain.entities.WasherCommission;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class ServiceOrderFinalizationIntegrationTest {

  @Test
  void finalizationPersistsPaymentStatusAndCommissionTogether() {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL(
        "jdbc:h2:mem:"
            + UUID.randomUUID().toString().replace("-", "")
            + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration-h2")
        .load()
        .migrate();
    Jdbi jdbi = Jdbi.create(dataSource);

    UUID orderId = UUID.randomUUID();
    UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    UUID washerId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    Instant now = Instant.now();
    ServiceOrder ready =
        new ServiceOrder(orderId, vehicleId, 1, washerId, ServiceOrderStatus.READY, now, now, now);
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO atendimentos
                      (id, veiculo_id, servico_preco_id, lavador_id, status, data_agendamento, criado_em, atualizado_em)
                    VALUES (:id, :vehicleId, 1, :washerId, 'PRONTO', :now, :now, :now)
                    """)
                .bind("id", orderId)
                .bind("vehicleId", vehicleId)
                .bind("washerId", washerId)
                .bind("now", now)
                .execute());

    ServiceOrder finalized =
        new ServiceOrderJdbiRepository(jdbi)
            .finalizeSale(
                ready,
                new ServicePayment(
                    UUID.randomUUID(), orderId, PaymentMethod.PIX, new BigDecimal("60.00"), now),
                new WasherCommission(
                    UUID.randomUUID(), orderId, washerId, new BigDecimal("20.00"), now));

    assertEquals(ServiceOrderStatus.DONE, finalized.status());
    assertEquals(
        Long.valueOf(1L),
        jdbi.withHandle(
            h -> h.createQuery("SELECT COUNT(*) FROM pagamentos").mapTo(Long.class).one()));
    assertEquals(
        Long.valueOf(1L),
        jdbi.withHandle(
            h -> h.createQuery("SELECT COUNT(*) FROM comissoes_lavador").mapTo(Long.class).one()));
    assertThrows(
        IllegalStateException.class,
        () ->
            new ServiceOrderJdbiRepository(jdbi)
                .finalizeSale(
                    ready,
                    new ServicePayment(
                        UUID.randomUUID(),
                        orderId,
                        PaymentMethod.PIX,
                        new BigDecimal("60.00"),
                        now),
                    new WasherCommission(
                        UUID.randomUUID(), orderId, washerId, new BigDecimal("20.00"), now)));
    assertEquals(
        Long.valueOf(1L),
        jdbi.withHandle(
            h -> h.createQuery("SELECT COUNT(*) FROM pagamentos").mapTo(Long.class).one()));
  }
}
