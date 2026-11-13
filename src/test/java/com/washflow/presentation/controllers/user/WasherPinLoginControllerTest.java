package com.washflow.presentation.controllers.user;

import static com.washflow.presentation.helpers.HttpRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.errors.user.AccountLockedError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.usecases.user.WasherBadge;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WasherPinLoginControllerTest {

  @Test
  void signsInWithPhoneAndPin() throws Exception {
    WasherBadge badge = mock(WasherBadge.class);
    User washer = mock(User.class);
    when(badge.signInWithPin("92999990002", "123456")).thenReturn(washer);

    HttpResponse response =
        new WasherPinLoginController(badge)
            .handle(request(Map.of(), "phone", "92999990002", "pin", "123456"));

    assertEquals(200, response.statusCode());
    assertEquals(washer, response.body());
  }

  @Test
  void mapsWrongPinLockAndWrongTypes() throws Exception {
    WasherBadge badge = mock(WasherBadge.class);
    when(badge.signInWithPin(any(), any()))
        .thenThrow(new InvalidCredentialsError())
        .thenThrow(new AccountLockedError());
    WasherPinLoginController sut = new WasherPinLoginController(badge);

    assertEquals(401, sut.handle(request(Map.of(), "phone", "1", "pin", "2")).statusCode());
    assertEquals(429, sut.handle(request(Map.of(), "phone", "1", "pin", "2")).statusCode());
    assertEquals(400, sut.handle(request(Map.of(), "phone", "1", "pin", 123456)).statusCode());
  }
}
