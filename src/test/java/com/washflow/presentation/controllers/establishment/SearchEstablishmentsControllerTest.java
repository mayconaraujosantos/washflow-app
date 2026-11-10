package com.washflow.presentation.controllers.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SearchEstablishmentsControllerTest {

  private static HttpRequest request(Map<String, String> path, Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, path, Map.of());
  }

  @Test
  void searchesActiveLavaJatosByName() {
    ManagePlatform platform = mock(ManagePlatform.class);
    when(platform.search("lava")).thenReturn(List.of());

    HttpResponse response =
        new SearchEstablishmentsController(platform)
            .handle(new HttpRequest(Map.of(), Map.of(), Map.of("query", "lava")));

    assertEquals(200, response.statusCode());
    verify(platform).search("lava");
  }
}
