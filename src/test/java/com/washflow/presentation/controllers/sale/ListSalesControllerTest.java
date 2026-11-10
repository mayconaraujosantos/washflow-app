package com.washflow.presentation.controllers.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.washflow.domain.entities.sale.SaleType;
import com.washflow.domain.usecases.sale.ListSales;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ListSalesControllerTest {

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
  void listsSalesOfARangeAndType() {
    ListSales list = mock(ListSales.class);

    HttpResponse response =
        new ListSalesController(list)
            .handle(query("from", "2026-11-01", "to", "2026-11-10", "type", "budget"));

    assertEquals(200, response.statusCode());
    verify(list)
        .list(
            new ListSales.Params(
                SaleType.BUDGET, LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-10")));
  }

  @Test
  void rejectsMissingOrInvalidDatesAndTypes() {
    ListSales list = mock(ListSales.class);
    ListSalesController sut = new ListSalesController(list);

    assertEquals(400, sut.handle(query("from", "2026-11-01")).statusCode());
    assertEquals(400, sut.handle(query("from", "ontem", "to", "2026-11-10")).statusCode());
    assertEquals(
        400, sut.handle(query("from", "2026-11-01", "to", "2026-11-10", "type", "X")).statusCode());
    verifyNoInteractions(list);
  }
}
