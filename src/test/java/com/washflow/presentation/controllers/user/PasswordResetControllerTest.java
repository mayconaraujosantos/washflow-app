package com.washflow.presentation.controllers.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.user.EmailDeliveryFailedError;
import com.washflow.domain.errors.user.EmailResetUnavailableError;
import com.washflow.domain.usecases.user.ResetPasswordByEmail;
import com.washflow.presentation.controllers.user.PasswordResetController.Who;
import com.washflow.presentation.protocols.HttpRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PasswordResetControllerTest {

  private static HttpRequest body(Map<String, Object> body) {
    return new HttpRequest(body, Map.of(), Map.of());
  }

  @Test
  void requestsByPhoneForCustomersAndByEmailForManagers() throws Exception {
    ResetPasswordByEmail reset = mock(ResetPasswordByEmail.class);
    when(reset.requestForCustomer("92991234567")).thenReturn(600);
    when(reset.requestForManager("rita@lava.test")).thenReturn(600);

    var customer =
        PasswordResetController.request(reset, Who.CUSTOMER)
            .handle(body(Map.of("phone", "92991234567")));
    var manager =
        PasswordResetController.request(reset, Who.MANAGER)
            .handle(body(Map.of("email", "rita@lava.test")));

    assertEquals(202, customer.statusCode());
    assertEquals(Map.of("expiresInSeconds", 600), customer.body());
    assertEquals(202, manager.statusCode());
  }

  @Test
  void confirmsTheNewPassword() throws Exception {
    ResetPasswordByEmail reset = mock(ResetPasswordByEmail.class);

    var response =
        PasswordResetController.confirm(reset, Who.MANAGER)
            .handle(
                body(Map.of("email", "rita@lava.test", "code", "123456", "password", "nova-123")));

    assertEquals(200, response.statusCode());
    verify(reset).confirmForManager("rita@lava.test", "123456", "nova-123");
  }

  @Test
  void wrongCodeIs401SoTheLoginThrottleCountsIt() throws Exception {
    ResetPasswordByEmail reset = mock(ResetPasswordByEmail.class);
    doThrow(new InvalidOtpError()).when(reset).confirmForCustomer(any(), any(), any());
    doThrow(new IllegalArgumentException("A nova senha deve ter no mínimo 8 caracteres"))
        .when(reset)
        .confirmForManager(any(), any(), any());

    assertEquals(
        401,
        PasswordResetController.confirm(reset, Who.CUSTOMER)
            .handle(body(Map.of("phone", "92991234567", "code", "000000", "password", "x")))
            .statusCode());
    assertEquals(
        400,
        PasswordResetController.confirm(reset, Who.MANAGER)
            .handle(body(Map.of("email", "rita@lava.test", "code", "1", "password", "x")))
            .statusCode());
  }

  @Test
  void noChannelOrMailServerDownIs503AndWrongTypesAre400() throws Exception {
    ResetPasswordByEmail reset = mock(ResetPasswordByEmail.class);
    when(reset.requestForCustomer(any()))
        .thenThrow(new EmailResetUnavailableError())
        .thenThrow(new EmailDeliveryFailedError());
    PasswordResetController sut = PasswordResetController.request(reset, Who.CUSTOMER);

    assertEquals(503, sut.handle(body(Map.of("phone", "92991234567"))).statusCode());
    assertEquals(503, sut.handle(body(Map.of("phone", "92991234567"))).statusCode());
    assertEquals(400, sut.handle(body(Map.of("phone", 92991234567L))).statusCode());
  }
}
