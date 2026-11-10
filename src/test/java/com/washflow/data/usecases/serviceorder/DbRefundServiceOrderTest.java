package com.washflow.data.usecases.serviceorder;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.RefundServiceOrderRepository;
import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.ServiceOrderRefund;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotRefundableError;
import com.washflow.domain.usecases.serviceorder.RefundServiceOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbRefundServiceOrderTest {

  @Mock private RefundServiceOrderRepository repository;

  private DbRefundServiceOrder sut;
  private final UUID orderId = UUID.randomUUID();
  private final UUID managerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sut = new DbRefundServiceOrder(repository);
  }

  @Test
  void refundsWithAListedReasonKeepingTheCommissionByDefault() throws Exception {
    ServiceOrderRefund refund = refund();
    when(repository.refund(orderId, managerId, "WRONG_CHARGE", null, null, false))
        .thenReturn(Optional.of(refund));

    assertSame(
        refund,
        sut.refund(
            new RefundServiceOrder.Params(orderId, managerId, " WRONG_CHARGE ", " ", null, false)));
  }

  @Test
  void passesTheChosenMethodAndCommissionReversal() throws Exception {
    ServiceOrderRefund refund = refund();
    when(repository.refund(orderId, managerId, "OTHER", "Riscou", PaymentMethod.CASH, true))
        .thenReturn(Optional.of(refund));

    assertSame(
        refund,
        sut.refund(
            new RefundServiceOrder.Params(
                orderId, managerId, "OTHER", "Riscou", PaymentMethod.CASH, true)));
  }

  @Test
  void refusesUnknownReasonsMissingDescriptionsAndLongTexts() {
    String[][] invalid = {
      {null, null},
      {"NO_SHOW", null},
      {"OTHER", null},
      {"OTHER", "  "},
      {"WRONG_CHARGE", "a".repeat(201)}
    };
    for (String[] input : invalid) {
      RefundServiceOrder.Params params =
          new RefundServiceOrder.Params(orderId, managerId, input[0], input[1], null, false);

      assertThrows(IllegalArgumentException.class, () -> sut.refund(params));
    }
    verifyNoInteractions(repository);
  }

  @Test
  void refusesOrdersThatAreNotFinalized() {
    when(repository.refund(any(), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(Optional.empty());

    assertThrows(
        ServiceOrderNotRefundableError.class,
        () ->
            sut.refund(
                new RefundServiceOrder.Params(
                    orderId, managerId, "SERVICE_NOT_DONE", null, null, false)));
  }

  private ServiceOrderRefund refund() {
    return new ServiceOrderRefund(
        UUID.randomUUID(),
        orderId,
        new BigDecimal("60.00"),
        PaymentMethod.PIX,
        "WRONG_CHARGE",
        null,
        BigDecimal.ZERO,
        managerId,
        Instant.now());
  }
}
