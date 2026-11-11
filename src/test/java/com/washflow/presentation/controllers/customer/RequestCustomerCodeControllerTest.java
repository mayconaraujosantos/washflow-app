package com.washflow.presentation.controllers.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.customer.OtpRecipient;
import com.washflow.domain.errors.auth.OtpTooSoonError;
import com.washflow.domain.errors.auth.OtpUnavailableError;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestCustomerCodeControllerTest {

  private static HttpRequest request(Map<String, String> path, Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, path, Map.of());
  }

  @Test
  void sendsACodeAndTellsWhenItExpires() throws Exception {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);
    when(login.requestCode(OtpRecipient.phone("92999990003"))).thenReturn(600);

    HttpResponse response =
        new RequestCustomerCodeController(login).handle(request(Map.of(), "phone", "92999990003"));

    assertEquals(200, response.statusCode());
    assertEquals(Map.of("expiresInSeconds", 600), response.body());
  }

  @Test
  void mapsTooSoonUnavailableAndWrongTypes() throws Exception {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);
    when(login.requestCode(any()))
        .thenThrow(new OtpTooSoonError())
        .thenThrow(new OtpUnavailableError());
    RequestCustomerCodeController sut = new RequestCustomerCodeController(login);

    assertEquals(429, sut.handle(request(Map.of(), "phone", "92999990003")).statusCode());
    assertEquals(503, sut.handle(request(Map.of(), "phone", "92999990003")).statusCode());
    assertEquals(400, sut.handle(request(Map.of(), "phone", 92999990003L)).statusCode());
    assertEquals(400, sut.handle(request(Map.of(), "phone", "1")).statusCode());
  }

  @Test
  void anEmailGoesToTheEmailChannel() throws Exception {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);
    when(login.requestCode(OtpRecipient.email("ana@email.com"))).thenReturn(600);
    RequestCustomerCodeController sut = new RequestCustomerCodeController(login);

    assertEquals(200, sut.handle(request(Map.of(), "email", " Ana@Email.com ")).statusCode());
    assertEquals(400, sut.handle(request(Map.of(), "email", "not-an-email")).statusCode());
    assertEquals(400, sut.handle(request(Map.of())).statusCode());
  }
}
