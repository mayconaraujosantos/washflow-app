package com.washflow.data.usecases.cancelserviceorder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.CancelServiceOrderRepository;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotCancellableError;
import com.washflow.domain.usecases.CancelServiceOrder;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbCancelServiceOrderTest {

  @Mock private CancelServiceOrderRepository repository;

  private DbCancelServiceOrder sut;
  private final UUID orderId = UUID.randomUUID();
  private final UUID managerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sut = new DbCancelServiceOrder(repository);
  }

  @Test
  void cancelsWithAListedReason() throws Exception {
    when(repository.cancel(any(), any(), any(), any())).thenReturn(true);

    sut.cancel(new CancelServiceOrder.Params(orderId, managerId, " NO_SHOW ", "  "));

    verify(repository).cancel(orderId, managerId, "NO_SHOW", null);
  }

  @Test
  void otherRequiresADescription() throws Exception {
    when(repository.cancel(any(), any(), any(), any())).thenReturn(true);

    sut.cancel(new CancelServiceOrder.Params(orderId, managerId, "OTHER", " Chuva forte "));

    verify(repository).cancel(orderId, managerId, "OTHER", "Chuva forte");
  }

  @Test
  void refusesUnknownReasonsMissingDescriptionsAndLongTexts() {
    String[][] invalid = {
      {null, null},
      {"", null},
      {"ANY", null},
      {"OTHER", null},
      {"OTHER", " "},
      {"NO_SHOW", "a".repeat(201)}
    };
    for (String[] input : invalid) {
      CancelServiceOrder.Params params =
          new CancelServiceOrder.Params(orderId, managerId, input[0], input[1]);

      assertThrows(IllegalArgumentException.class, () -> sut.cancel(params));
    }
    verifyNoInteractions(repository);
  }

  @Test
  void refusesOrdersThatCanNoLongerBeCancelled() {
    when(repository.cancel(any(), any(), any(), any())).thenReturn(false);

    assertThrows(
        ServiceOrderNotCancellableError.class,
        () ->
            sut.cancel(
                new CancelServiceOrder.Params(orderId, managerId, "CUSTOMER_GAVE_UP", null)));
  }
}
