package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.washflow.domain.entities.sale.Sale;
import com.washflow.domain.entities.sale.SaleItem;
import com.washflow.domain.entities.sale.SalePayment;
import com.washflow.domain.entities.sale.SaleRefund;
import com.washflow.domain.entities.sale.SaleType;
import com.washflow.domain.entities.shared.PaymentMethod;
import com.washflow.domain.errors.sale.SaleNotFoundError;
import com.washflow.domain.errors.sale.SaleRefundNotAllowedError;
import com.washflow.infra.db.jdbi.sale.SaleJdbiRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SaleRefundIntegrationTest {

  private static final UUID MANAGER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

  private Jdbi jdbi;
  private SaleJdbiRepository repository;

  @BeforeEach
  void setUp() {
    jdbi = TestDatabase.prepare();
    repository = new SaleJdbiRepository(jdbi);
  }

  @Test
  void refundsPartOfAPaymentMethodUpToWhatCameInThroughIt() throws Exception {
    UUID saleId = sale(SaleType.QUICK_SALE);

    Sale afterPix = repository.refund(saleId, refund(saleId, PaymentMethod.PIX, "30.00"));
    assertEquals(new BigDecimal("30.00"), afterPix.refundedAmount());
    assertEquals(1, afterPix.refunds().size());

    SaleRefundNotAllowedError tooMuch =
        assertThrows(
            SaleRefundNotAllowedError.class,
            () -> repository.refund(saleId, refund(saleId, PaymentMethod.PIX, "20.01")));
    assertEquals("Pix só tem R$ 20,00 para estornar nesta venda", tooMuch.getMessage());
    assertThrows(
        SaleRefundNotAllowedError.class,
        () -> repository.refund(saleId, refund(saleId, PaymentMethod.DEBIT_CARD, "1.00")));

    Sale full = repository.refund(saleId, refund(saleId, PaymentMethod.CASH, "20.00"));
    repository.refund(saleId, refund(saleId, PaymentMethod.PIX, "20.00"));

    LocalDate day = LocalDate.now(ZoneId.of("America/Manaus"));
    Sale listed =
        repository
            .loadBetween(
                day.atStartOfDay(ZoneId.of("America/Manaus")).toInstant(),
                day.plusDays(1).atStartOfDay(ZoneId.of("America/Manaus")).toInstant(),
                null)
            .getFirst();
    assertEquals(new BigDecimal("50.00"), full.refundedAmount());
    assertEquals(new BigDecimal("70.00"), listed.refundedAmount());
    assertEquals(3, listed.refunds().size());
    assertEquals(new BigDecimal("70.00"), listed.paidAmount());
  }

  @Test
  void listsRefundsByTheDayTheyWereMade() throws Exception {
    UUID saleId = sale(SaleType.QUICK_SALE);
    repository.refund(saleId, refund(saleId, PaymentMethod.PIX, "10.00"));
    Instant now = Instant.now();

    List<SaleRefund> today =
        repository.loadRefundsBetween(now.minusSeconds(3600), now.plusSeconds(3600));
    List<SaleRefund> yesterday =
        repository.loadRefundsBetween(now.minusSeconds(90_000), now.minusSeconds(3600));

    assertEquals(1, today.size());
    assertEquals(saleId, today.getFirst().saleId());
    assertEquals(new BigDecimal("10.00"), today.getFirst().amount());
    assertEquals(0, yesterday.size());
  }

  @Test
  void budgetsAndUnknownSalesCannotBeRefunded() {
    UUID budget = sale(SaleType.BUDGET);

    assertThrows(
        SaleRefundNotAllowedError.class,
        () -> repository.refund(budget, refund(budget, PaymentMethod.PIX, "1.00")));
    assertThrows(
        SaleNotFoundError.class,
        () -> repository.refund(UUID.randomUUID(), refund(budget, PaymentMethod.PIX, "1.00")));
  }

  @Test
  void refundsCannotBeEditedOrDeleted() throws Exception {
    UUID saleId = sale(SaleType.QUICK_SALE);
    repository.refund(saleId, refund(saleId, PaymentMethod.PIX, "1.00"));

    assertThrows(
        Exception.class,
        () -> jdbi.useHandle(h -> h.execute("UPDATE venda_estornos SET valor = 2")));
    assertThrows(
        Exception.class, () -> jdbi.useHandle(h -> h.execute("DELETE FROM venda_estornos")));
  }

  private static SaleRefund refund(UUID saleId, PaymentMethod method, String amount) {
    return new SaleRefund(
        UUID.randomUUID(),
        saleId,
        method,
        new BigDecimal(amount),
        "WRONG_CHARGE",
        null,
        MANAGER_ID,
        Instant.now());
  }

  /** A sale of 70.00 paid with PIX 50.00 + CASH 20.00 (a budget has no payments). */
  private UUID sale(SaleType type) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    List<SalePayment> payments =
        type == SaleType.BUDGET
            ? List.of()
            : List.of(
                new SalePayment(UUID.randomUUID(), PaymentMethod.PIX, new BigDecimal("50.00"), now),
                new SalePayment(
                    UUID.randomUUID(), PaymentMethod.CASH, new BigDecimal("20.00"), now));
    repository.save(
        Sale.of(
            id,
            type,
            null,
            null,
            null,
            BigDecimal.ZERO,
            null,
            MANAGER_ID,
            now,
            List.of(
                new SaleItem(
                    UUID.randomUUID(),
                    1,
                    "Lavagem Completa",
                    1,
                    new BigDecimal("70.00"),
                    new BigDecimal("70.00"))),
            payments));
    return id;
  }
}
