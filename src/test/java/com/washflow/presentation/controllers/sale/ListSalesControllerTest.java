package com.washflow.presentation.controllers.sale;

import static com.washflow.presentation.helpers.HttpRequests.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.washflow.domain.entities.sale.SaleType;
import com.washflow.domain.usecases.sale.ListSales;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ListSalesControllerTest {

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
