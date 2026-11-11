package com.washflow.presentation.controllers.customer;

import static com.washflow.presentation.helpers.HttpRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.customer.OtpRecipient;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.user.UserProfileMismatchError;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VerifyCustomerCodeControllerTest {

  @Test
  void signsInWithAValidCode() throws Exception {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);
    User customer = mock(User.class);
    when(login.verifyCode(OtpRecipient.phone("92999990003"), "123456", "Ana")).thenReturn(customer);

    HttpResponse response =
        new VerifyCustomerCodeController(login)
            .handle(request(Map.of(), "phone", "92999990003", "code", "123456", "name", "Ana"));

    assertEquals(200, response.statusCode());
    assertEquals(customer, response.body());
  }

  @Test
  void signsInWithACodeSentByEmail() throws Exception {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);
    User customer = mock(User.class);
    when(login.verifyCode(OtpRecipient.email("ana@email.com"), "123456", null))
        .thenReturn(customer);

    HttpResponse response =
        new VerifyCustomerCodeController(login)
            .handle(request(Map.of(), "email", "ana@email.com", "code", "123456"));

    assertEquals(200, response.statusCode());
    assertEquals(customer, response.body());
  }

  @Test
  void asksForTheNameOfANewPhone() throws Exception {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);
    when(login.verifyCode(any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Informe o nome"));

    HttpResponse response =
        new VerifyCustomerCodeController(login)
            .handle(request(Map.of(), "phone", "92999990003", "code", "123456"));

    assertEquals(400, response.statusCode());
    assertEquals(true, ((Map<?, ?>) response.body()).get("needsName"));
  }

  @Test
  void aWrongTypeIsABadRequestThatDoesNotAskForTheName() {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);

    HttpResponse response =
        new VerifyCustomerCodeController(login)
            .handle(request(Map.of(), "phone", "92999990003", "code", 123456));

    assertEquals(400, response.statusCode());
    assertEquals(null, ((Map<?, ?>) response.body()).get("needsName"));
    verifyNoInteractions(login);
  }

  @Test
  void mapsAWrongCodeAndAStaffPhone() throws Exception {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);
    when(login.verifyCode(any(), any(), any()))
        .thenThrow(new InvalidOtpError())
        .thenThrow(
            new UserProfileMismatchError(
                "1",
                com.washflow.domain.entities.user.UserProfile.MANAGER,
                com.washflow.domain.entities.user.UserProfile.CUSTOMER));
    VerifyCustomerCodeController sut = new VerifyCustomerCodeController(login);

    assertEquals(
        401, sut.handle(request(Map.of(), "phone", "92999990003", "code", "1")).statusCode());
    assertEquals(
        409, sut.handle(request(Map.of(), "phone", "92999990003", "code", "1")).statusCode());
  }
}
