package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.domain.entities.finance.OperationsReport;
import com.washflow.domain.entities.serviceorder.ServiceOrderRefund;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.shared.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceOrderRefundIntegrationTest {

  private static final UUID VEHICLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID WASHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID MANAGER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final ZoneId MANAUS = ZoneId.of("America/Manaus");

  private Jdbi jdbi;
  private ServiceOrderJdbiRepository repository;

  @BeforeEach
  void setUp() {
    jdbi = TestDatabase.prepare();
    repository = new ServiceOrderJdbiRepository(jdbi);
  }

  @Test
  void refundsTheFullPaymentInTheOriginalMethodKeepingTheCommission() {
    UUID id = finalizedOrder("FINALIZADO", Instant.now());

    ServiceOrderRefund refund =
        repository.refund(id, MANAGER_ID, "CUSTOMER_COMPLAINT", null, null, false).orElseThrow();

    assertEquals(id, refund.serviceOrderId());
    assertEquals(new BigDecimal("60.00"), refund.amount());
    assertEquals(PaymentMethod.PIX, refund.method());
    assertEquals(BigDecimal.ZERO.setScale(2), refund.reversedCommission());
    assertEquals(MANAGER_ID, refund.createdBy());
    assertEquals(ServiceOrderStatus.REFUNDED, repository.loadById(id).orElseThrow().status());
  }

  @Test
  void canReverseTheCommissionAndRefundInAnotherMethod() {
    UUID id = finalizedOrder("FINALIZADO", Instant.now());

    ServiceOrderRefund refund =
        repository
            .refund(id, MANAGER_ID, "OTHER", "Carro riscado", PaymentMethod.CASH, true)
            .orElseThrow();

    assertEquals(PaymentMethod.CASH, refund.method());
    assertEquals(new BigDecimal("20.00"), refund.reversedCommission());
    assertEquals("Carro riscado", refund.description());
  }

  @Test
  void onlyFinalizedOrdersAreRefundedAndOnlyOnce() {
    UUID ready = finalizedOrder("PRONTO", Instant.now());
    UUID done = finalizedOrder("FINALIZADO", Instant.now());

    assertTrue(repository.refund(ready, MANAGER_ID, "WRONG_CHARGE", null, null, false).isEmpty());
    assertTrue(repository.refund(done, MANAGER_ID, "WRONG_CHARGE", null, null, false).isPresent());
    assertTrue(repository.refund(done, MANAGER_ID, "WRONG_CHARGE", null, null, false).isEmpty());
  }

  @Test
  void refundsCannotBeEditedOrDeleted() {
    UUID id = finalizedOrder("FINALIZADO", Instant.now());
    repository.refund(id, MANAGER_ID, "WRONG_CHARGE", null, null, false);

    assertThrows(
        Exception.class,
        () -> jdbi.useHandle(h -> h.execute("UPDATE estornos_atendimento SET valor = 1")));
    assertThrows(
        Exception.class, () -> jdbi.useHandle(h -> h.execute("DELETE FROM estornos_atendimento")));
  }

  @Test
  void reportKeepsPastRevenueAndSubtractsRefundsOnTheRefundDay() {
    LocalDate today = LocalDate.now(MANAUS);
    LocalDate lastWeek = today.minusDays(7);
    UUID oldOrder = finalizedOrder("FINALIZADO", lastWeek.atTime(10, 0).atZone(MANAUS).toInstant());
    finalizedOrder("FINALIZADO", Instant.now());

    repository.refund(oldOrder, MANAGER_ID, "CUSTOMER_COMPLAINT", null, null, true);

    OperationsReport past =
        new OperationsReportJdbiRepository(jdbi).loadOperationsReport(lastWeek, lastWeek);
    assertEquals(new BigDecimal("60.00"), past.revenue());
    assertEquals(0, past.refundedOrders());
    assertEquals(new BigDecimal("60.00"), past.netRevenue());

    OperationsReport now =
        new OperationsReportJdbiRepository(jdbi).loadOperationsReport(today, today);
    assertEquals(new BigDecimal("60.00"), now.revenue());
    assertEquals(1, now.refundedOrders());
    assertEquals(new BigDecimal("60.00"), now.refunds());
    assertEquals(new BigDecimal("0.00"), now.netRevenue());
    assertEquals(new BigDecimal("20.00"), now.reversedCommissions());
    assertEquals(new BigDecimal("0.00"), now.netCommissions());
    assertEquals("CUSTOMER_COMPLAINT", now.refundReasons().getFirst().reason());
    assertEquals(1, now.refundReasons().getFirst().count());
  }

  /** An order of 60.00 paid by PIX at {@code paidAt}, with a 20.00 commission. */
  private UUID finalizedOrder(String status, Instant paidAt) {
    UUID id = UUID.randomUUID();
    jdbi.useHandle(
        handle -> {
          handle
              .createUpdate(
                  """
                  INSERT INTO atendimentos
                    (id, estabelecimento_id, veiculo_id, servico_preco_id, lavador_id, status,
                     data_agendamento, criado_em, atualizado_em)
                  VALUES (:id, :tenant, :vehicleId, 1, :washerId, :status, :at, :at, :at)
                  """)
              .bind("id", id)
              .bind("vehicleId", VEHICLE_ID)
              .bind("washerId", WASHER_ID)
              .bind("status", status)
              .bind("at", paidAt)
              .execute();
          if ("FINALIZADO".equals(status)) {
            handle
                .createUpdate(
                    """
                    INSERT INTO pagamentos (id, atendimento_id, forma_pagamento, valor, recebido_em)
                    VALUES (:pid, :id, 'PIX', 60.00, :at)
                    """)
                .bind("pid", UUID.randomUUID())
                .bind("id", id)
                .bind("at", paidAt)
                .execute();
            handle
                .createUpdate(
                    """
                    INSERT INTO comissoes_lavador (id, atendimento_id, lavador_id, valor, criado_em)
                    VALUES (:cid, :id, :washerId, 20.00, :at)
                    """)
                .bind("cid", UUID.randomUUID())
                .bind("id", id)
                .bind("washerId", WASHER_ID)
                .bind("at", paidAt)
                .execute();
          }
        });
    return id;
  }
}
