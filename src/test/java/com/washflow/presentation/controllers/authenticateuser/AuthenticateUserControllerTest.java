package com.washflow.presentation.controllers.authenticateuser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.InvalidCredentialsError;
import com.washflow.domain.errors.ManagerSelfRegistrationNotAllowedError;
import com.washflow.domain.errors.UserProfileMismatchError;
import com.washflow.domain.errors.WasherSelfRegistrationNotAllowedError;
import com.washflow.domain.usecases.AuthenticateUser;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserControllerTest {

  @Mock private AuthenticateUser authenticateUser;
  @Mock private Validation validation;

  private AuthenticateUserController sut;

  @BeforeEach
  void setUp() {
    sut = new AuthenticateUserController(authenticateUser, validation, UserProfile.CUSTOMER);
  }

  @Test
  void authenticatesUserAndReturnsOk() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("phone", "11999999999");
    body.put("name", "Test User");
    body.put("password", "password123");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    User saved =
        new User(
            UUID.randomUUID(),
            "Test User",
            "11999999999",
            UserProfile.CUSTOMER,
            Instant.now(),
            true);
    when(authenticateUser.authenticate(any(AuthenticateUser.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(authenticateUser).authenticate(any(AuthenticateUser.Params.class));
  }

  @Test
  void returnsBadRequestWhenValidationFails() {
    Map<String, Object> body = new HashMap<>();
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(new IllegalArgumentException("missing field"));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenPasswordTooShort() {
    Map<String, Object> body = new HashMap<>();
    body.put("phone", "11999999999");
    body.put("name", "Test User");
    body.put("password", "short");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsUnauthorizedWhenInvalidCredentials() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("phone", "11999999999");
    body.put("name", "Test User");
    body.put("password", "password123");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(authenticateUser.authenticate(any(AuthenticateUser.Params.class)))
        .thenThrow(new InvalidCredentialsError());

    HttpResponse response = sut.handle(request);

    assertEquals(401, response.statusCode());
  }

  @Test
  void returnsConflictWhenProfileMismatch() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("phone", "11999999999");
    body.put("name", "Test User");
    body.put("password", "password123");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(authenticateUser.authenticate(any(AuthenticateUser.Params.class)))
        .thenThrow(
            new UserProfileMismatchError("CUSTOMER", UserProfile.CUSTOMER, UserProfile.MANAGER));

    HttpResponse response = sut.handle(request);

    assertEquals(409, response.statusCode());
  }

  @Test
  void returnsForbiddenWhenManagerSelfRegistration() throws Exception {
    AuthenticateUserController managerSut =
        new AuthenticateUserController(authenticateUser, validation, UserProfile.MANAGER);

    Map<String, Object> body = new HashMap<>();
    body.put("phone", "11999999999");
    body.put("name", "Test Manager");
    body.put("password", "password123");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(authenticateUser.authenticate(any(AuthenticateUser.Params.class)))
        .thenThrow(new ManagerSelfRegistrationNotAllowedError("11999999999"));

    HttpResponse response = managerSut.handle(request);

    assertEquals(403, response.statusCode());
  }

  @Test
  void returnsForbiddenWhenWasherSelfRegistration() throws Exception {
    AuthenticateUserController washerSut =
        new AuthenticateUserController(authenticateUser, validation, UserProfile.WASHER);

    Map<String, Object> body = new HashMap<>();
    body.put("phone", "11999999999");
    body.put("name", "Test Washer");
    body.put("password", "password123");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(authenticateUser.authenticate(any(AuthenticateUser.Params.class)))
        .thenThrow(new WasherSelfRegistrationNotAllowedError("11999999999"));

    HttpResponse response = washerSut.handle(request);

    assertEquals(403, response.statusCode());
  }
}
