package com.washflow.data.usecases.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.RefundSaleRepository;
import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.Sale;
import com.washflow.domain.entities.SaleRefund;
import com.washflow.domain.usecases.sale.RefundSale;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbRefundSaleTest {

  private static final Instant NOW = Instant.parse("2026-11-10T15:00:00Z");

  @Mock private RefundSaleRepository repository;
  @Mock private Sale sale;

  private DbRefundSale sut;
  private final UUID saleId = UUID.randomUUID();
  private final UUID managerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sut = new DbRefundSale(repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void recordsTheRefundByTheManagerNow() throws Exception {
    when(repository.refund(eq(saleId), any())).thenReturn(sale);

    assertSame(
        sale,
        sut.refund(
            new RefundSale.Params(
                saleId,
                managerId,
                PaymentMethod.PIX,
                new BigDecimal("12.5"),
                " WRONG_CHARGE ",
                " ")));

    ArgumentCaptor<SaleRefund> refund = ArgumentCaptor.forClass(SaleRefund.class);
    verify(repository).refund(eq(saleId), refund.capture());
    assertEquals(saleId, refund.getValue().saleId());
    assertEquals(PaymentMethod.PIX, refund.getValue().method());
    assertEquals(new BigDecimal("12.50"), refund.getValue().amount());
    assertEquals("WRONG_CHARGE", refund.getValue().reason());
    assertEquals(null, refund.getValue().description());
    assertEquals(managerId, refund.getValue().createdBy());
    assertEquals(NOW, refund.getValue().createdAt());
  }

  @Test
  void refusesInvalidMethodAmountOrReason() {
    Object[][] invalid = {
      {null, "10", "WRONG_CHARGE", null},
      {PaymentMethod.PIX, null, "WRONG_CHARGE", null},
      {PaymentMethod.PIX, "0", "WRONG_CHARGE", null},
      {PaymentMethod.PIX, "-1", "WRONG_CHARGE", null},
      {PaymentMethod.PIX, "1.005", "WRONG_CHARGE", null},
      {PaymentMethod.PIX, "10", "NO_SHOW", null},
      {PaymentMethod.PIX, "10", "OTHER", " "},
      {PaymentMethod.PIX, "10", "WRONG_CHARGE", "a".repeat(201)}
    };
    for (Object[] input : invalid) {
      RefundSale.Params params =
          new RefundSale.Params(
              saleId,
              managerId,
              (PaymentMethod) input[0],
              input[1] == null ? null : new BigDecimal((String) input[1]),
              (String) input[2],
              (String) input[3]);

      assertThrows(IllegalArgumentException.class, () -> sut.refund(params));
    }
    verifyNoInteractions(repository);
  }
}
