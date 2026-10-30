package com.washflow.presentation.controllers.createwasher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.PhoneAlreadyRegisteredError;
import com.washflow.domain.usecases.CreateWasher;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
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
class CreateWasherControllerTest {

  @Mock private CreateWasher createWasher;

  private CreateWasherController sut;

  @BeforeEach
  void setUp() {
    sut = new CreateWasherController(createWasher);
  }

  @Test
  void createsWasherAndReturnsCreated() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("name", "João Lavador");
    body.put("phone", "11999999999");
    body.put("password", "password123");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    User saved =
        new User(
            UUID.randomUUID(),
            "João Lavador",
            "11999999999",
            UserProfile.WASHER,
            Instant.now(),
            true);
    when(createWasher.create(any(CreateWasher.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(201, response.statusCode());
    verify(createWasher).create(any(CreateWasher.Params.class));
  }

  @Test
  void returnsServerErrorWhenNameMissing() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("phone", "11999999999");
    body.put("password", "password123");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(createWasher.create(any(CreateWasher.Params.class)))
        .thenThrow(new IllegalArgumentException("name is required"));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsConflictWhenPhoneAlreadyRegistered() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("name", "João Lavador");
    body.put("phone", "11999999999");
    body.put("password", "password123");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(createWasher.create(any(CreateWasher.Params.class)))
        .thenThrow(new PhoneAlreadyRegisteredError());

    HttpResponse response = sut.handle(request);

    assertEquals(409, response.statusCode());
  }
}
