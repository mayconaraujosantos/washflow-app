package com.washflow.presentation.controllers.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.usecases.user.WasherBadge;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WasherBadgeLoginControllerTest {

  private static HttpRequest request(Map<String, String> path, Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, path, Map.of());
  }

  @Test
  void signsInWithTheBadgeToken() throws Exception {
    WasherBadge badge = mock(WasherBadge.class);
    User washer = mock(User.class);
    when(badge.signInWithBadge("qr-token")).thenReturn(washer);

    HttpResponse response =
        new WasherBadgeLoginController(badge).handle(request(Map.of(), "badgeToken", "qr-token"));

    assertEquals(200, response.statusCode());
    assertEquals(washer, response.body());
  }

  @Test
  void mapsAnUnknownBadgeAndWrongTypes() throws Exception {
    WasherBadge badge = mock(WasherBadge.class);
    when(badge.signInWithBadge(any())).thenThrow(new InvalidCredentialsError());
    WasherBadgeLoginController sut = new WasherBadgeLoginController(badge);

    assertEquals(401, sut.handle(request(Map.of(), "badgeToken", "x")).statusCode());
    assertEquals(400, sut.handle(request(Map.of(), "badgeToken", 1)).statusCode());
  }
}
