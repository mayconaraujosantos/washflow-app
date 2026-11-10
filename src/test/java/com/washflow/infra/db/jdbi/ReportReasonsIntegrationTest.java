package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.washflow.domain.entities.OperationsReport;
import com.washflow.domain.entities.OperationsReport.ReasonCount;
import com.washflow.domain.entities.OperationsReport.RefundBreakdown;
import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.Sale;
import com.washflow.domain.entities.SaleItem;
import com.washflow.domain.entities.SalePayment;
import com.washflow.domain.entities.SaleRefund;
import com.washflow.domain.entities.SaleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The report's cancellation reasons and quick-sale totals, counted on the day they happened. */
class ReportReasonsIntegrationTest {

  private static final UUID VEHICLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID MANAGER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final ZoneId MANAUS = ZoneId.of("America/Manaus");

  private Jdbi jdbi;
  private ServiceOrderJdbiRepository orders;
  private SaleJdbiRepository sales;
  private LocalDate today;

  @BeforeEach
  void setUp() {
    jdbi = TestDatabase.prepare();
    orders = new ServiceOrderJdbiRepository(jdbi);
    sales = new SaleJdbiRepository(jdbi);
    today = LocalDate.now(MANAUS);
  }

  @Test
  void countsCancellationsByReasonOnTheCancellationDay() {
    orders.cancel(order(), MANAGER_ID, "NO_SHOW", null);
    orders.cancel(order(), MANAGER_ID, "NO_SHOW", null);
    orders.cancel(order(), MANAGER_ID, "CUSTOMER_GAVE_UP", null);

    OperationsReport report = orders.loadOperationsReport(today, today);
    OperationsReport yesterday =
        orders.loadOperationsReport(today.minusDays(1), today.minusDays(1));

    assertEquals(3, report.cancelledOrders());
    assertEquals(
        List.of(new ReasonCount("NO_SHOW", 2), new ReasonCount("CUSTOMER_GAVE_UP", 1)),
        report.cancellationReasons());
    assertEquals(0, yesterday.cancelledOrders());
  }

  @Test
  void summarizesQuickSalesReceivedAndRefundedInThePeriod() throws Exception {
    Instant lastWeek = today.minusDays(7).atTime(10, 0).atZone(MANAUS).toInstant();
    UUID oldSale = sale(lastWeek, "80.00");
    UUID newSale = sale(Instant.now(), "50.00");
    sales.refund(oldSale, refund(oldSale, "30.00", "CUSTOMER_COMPLAINT"));
    sales.refund(newSale, refund(newSale, "10.00", "WRONG_CHARGE"));

    OperationsReport report = orders.loadOperationsReport(today, today);
    OperationsReport past = orders.loadOperationsReport(today.minusDays(7), today.minusDays(7));

    assertEquals(new BigDecimal("50.00"), report.salesReceived());
    assertEquals(new BigDecimal("40.00"), report.saleRefunds());
    assertEquals(new BigDecimal("10.00"), report.netSales());
    assertEquals(
        List.of(
            new RefundBreakdown("CUSTOMER_COMPLAINT", 1, new BigDecimal("30.00")),
            new RefundBreakdown("WRONG_CHARGE", 1, new BigDecimal("10.00"))),
        report.saleRefundReasons());
    assertEquals(new BigDecimal("80.00"), past.salesReceived());
    assertEquals(new BigDecimal("80.00"), past.netSales());
  }

  private UUID order() {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO atendimentos
                      (id, estabelecimento_id, veiculo_id, servico_preco_id, status,
                       data_agendamento, criado_em, atualizado_em)
                    VALUES (:id, :tenant, :vehicleId, 1, 'AGENDADO', :now, :now, :now)
                    """)
                .bind("id", id)
                .bind("vehicleId", VEHICLE_ID)
                .bind("now", now)
                .execute());
    return id;
  }

  private UUID sale(Instant at, String amount) {
    UUID id = UUID.randomUUID();
    BigDecimal value = new BigDecimal(amount);
    sales.save(
        Sale.of(
            id,
            SaleType.QUICK_SALE,
            null,
            null,
            null,
            BigDecimal.ZERO,
            null,
            MANAGER_ID,
            at,
            List.of(new SaleItem(UUID.randomUUID(), 1, "Lavagem Completa", 1, value, value)),
            List.of(new SalePayment(UUID.randomUUID(), PaymentMethod.PIX, value, at))));
    return id;
  }

  private static SaleRefund refund(UUID saleId, String amount, String reason) {
    return new SaleRefund(
        UUID.randomUUID(),
        saleId,
        PaymentMethod.PIX,
        new BigDecimal(amount),
        reason,
        null,
        MANAGER_ID,
        Instant.now());
  }
}
