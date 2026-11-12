package com.washflow.presentation.controllers.vehicle;

import static com.washflow.presentation.helpers.HttpRequests.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.vehicle.SearchVehicles;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchVehiclesControllerTest {

  @Test
  void searchesByThePlateNameOrPhoneQuery() {
    SearchVehicles search = mock(SearchVehicles.class);
    when(search.search("ABC")).thenReturn(List.of());

    HttpResponse response = new SearchVehiclesController(search).handle(query("query", "ABC"));

    assertEquals(200, response.statusCode());
    assertEquals(List.of(), response.body());
  }
}
