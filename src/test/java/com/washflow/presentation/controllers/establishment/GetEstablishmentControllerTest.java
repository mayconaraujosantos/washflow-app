package com.washflow.presentation.controllers.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.establishment.Establishment;
import com.washflow.domain.usecases.establishment.ManageEstablishment;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GetEstablishmentControllerTest {

  private static HttpRequest request(Map<String, String> path, Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, path, Map.of());
  }

  @Test
  void returnsTheLavaJatoOfTheRequest() {
    ManageEstablishment manage = mock(ManageEstablishment.class);
    Establishment establishment = mock(Establishment.class);
    when(manage.get()).thenReturn(establishment);

    HttpResponse response = new GetEstablishmentController(manage).handle(request(Map.of()));

    assertEquals(200, response.statusCode());
    assertEquals(establishment, response.body());
  }
}
