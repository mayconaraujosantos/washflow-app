package com.washflow.presentation.controllers.deletecashtransaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.washflow.domain.usecases.DeleteCashTransaction;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteCashTransactionControllerTest {

  @Mock private DeleteCashTransaction deleteCashTransaction;

  private DeleteCashTransactionController sut;
  private UUID transactionId;

  @BeforeEach
  void setUp() {
    sut = new DeleteCashTransactionController(deleteCashTransaction);
    transactionId = UUID.randomUUID();
  }

  @Test
  void deletesTransactionAndReturnsNoContent() {
    HttpRequest request =
        new HttpRequest(Collections.emptyMap(), Map.of("id", transactionId.toString()), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(204, response.statusCode());
    verify(deleteCashTransaction).delete(transactionId);
  }

  @Test
  void returnsBadRequestWhenInvalidId() {
    HttpRequest request =
        new HttpRequest(Collections.emptyMap(), Map.of("id", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
