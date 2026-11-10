package com.washflow.presentation.controllers.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.customer.CustomerRecoveryEmail;
import com.washflow.presentation.protocols.HttpRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerRecoveryEmailControllerTest {

  private final UUID id = UUID.randomUUID();

  private HttpRequest request(Map<String, Object> body) {
    return new HttpRequest(body, Map.of("id", id.toString()), Map.of());
  }

  @Test
  void showsAndSavesTheRecoveryEmail() {
    CustomerRecoveryEmail recovery = mock(CustomerRecoveryEmail.class);
    when(recovery.load(id)).thenReturn(Optional.empty());
    when(recovery.update(id, "ana@mail.test")).thenReturn(Optional.of("ana@mail.test"));

    var loaded = CustomerRecoveryEmailController.load(recovery).handle(request(Map.of()));
    var saved =
        CustomerRecoveryEmailController.update(recovery)
            .handle(request(Map.of("email", "ana@mail.test")));

    Map<String, Object> empty = new HashMap<>();
    empty.put("email", null);
    assertEquals(200, loaded.statusCode());
    assertEquals(empty, loaded.body());
    assertEquals(Map.of("email", "ana@mail.test"), saved.body());
  }

  @Test
  void invalidEmailIs400() {
    CustomerRecoveryEmail recovery = mock(CustomerRecoveryEmail.class);
    when(recovery.update(any(), any())).thenThrow(new IllegalArgumentException("E-mail inválido"));

    assertEquals(
        400,
        CustomerRecoveryEmailController.update(recovery)
            .handle(request(Map.of("email", "sem-arroba")))
            .statusCode());
  }
}
