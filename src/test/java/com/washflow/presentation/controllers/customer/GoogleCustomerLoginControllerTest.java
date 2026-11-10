package com.washflow.presentation.controllers.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.errors.auth.GoogleLoginUnavailableError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.errors.user.UserProfileMismatchError;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoogleCustomerLoginControllerTest {

  private static HttpRequest request(Map<String, String> path, Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, path, Map.of());
  }

  @Test
  void signsInWithAGoogleCredential() throws Exception {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);
    User customer = mock(User.class);
    when(login.signInWithGoogle("jwt")).thenReturn(customer);

    HttpResponse response =
        new GoogleCustomerLoginController(login).handle(request(Map.of(), "credential", "jwt"));

    assertEquals(200, response.statusCode());
    assertEquals(customer, response.body());
  }

  @Test
  void mapsUnavailableInvalidStaffAndWrongTypes() throws Exception {
    CustomerPasswordlessLogin login = mock(CustomerPasswordlessLogin.class);
    when(login.signInWithGoogle(any()))
        .thenThrow(new GoogleLoginUnavailableError())
        .thenThrow(new InvalidCredentialsError())
        .thenThrow(
            new UserProfileMismatchError(
                "1",
                com.washflow.domain.entities.user.UserProfile.MANAGER,
                com.washflow.domain.entities.user.UserProfile.CUSTOMER));
    GoogleCustomerLoginController sut = new GoogleCustomerLoginController(login);

    assertEquals(503, sut.handle(request(Map.of(), "credential", "x")).statusCode());
    assertEquals(401, sut.handle(request(Map.of(), "credential", "x")).statusCode());
    assertEquals(409, sut.handle(request(Map.of(), "credential", "x")).statusCode());
    assertEquals(400, sut.handle(request(Map.of(), "credential", 1)).statusCode());
  }
}
