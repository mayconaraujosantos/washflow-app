package com.washflow.presentation.controllers.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.vehicle.SearchVehicles;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SearchVehiclesControllerTest {

  private static HttpRequest body(Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, Map.of(), Map.of());
  }

  private static HttpRequest query(String... pairs) {
    Map<String, String> query = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      query.put(pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(Map.of(), Map.of(), query);
  }

  @Test
  void searchesByThePlateNameOrPhoneQuery() {
    SearchVehicles search = mock(SearchVehicles.class);
    when(search.search("ABC")).thenReturn(List.of());

    HttpResponse response = new SearchVehiclesController(search).handle(query("query", "ABC"));

    assertEquals(200, response.statusCode());
    assertEquals(List.of(), response.body());
  }
}
