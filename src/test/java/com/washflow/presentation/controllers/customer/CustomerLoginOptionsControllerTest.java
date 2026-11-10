package com.washflow.presentation.controllers.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.domain.usecases.user.ResetPasswordByEmail;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CustomerLoginOptionsControllerTest {

  private static HttpRequest request(Map<String, String> path, Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, path, Map.of());
  }

  @Test
  void returnsTheEnabledSignInOptions() {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);
    CustomerPasswordlessLogin.Options options =
        new CustomerPasswordlessLogin.Options(true, "client-id");
    when(login.options()).thenReturn(options);

    ResetPasswordByEmail reset = mock(ResetPasswordByEmail.class);
    when(reset.available()).thenReturn(true);

    HttpResponse response =
        new CustomerLoginOptionsController(login, reset).handle(request(Map.of()));

    assertEquals(200, response.statusCode());
    assertEquals(
        Map.of("whatsappCode", true, "googleClientId", "client-id", "emailCode", true),
        response.body());
  }
}
