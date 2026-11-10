package com.washflow.presentation.controllers.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.sale.SaleRefund;
import com.washflow.domain.usecases.sale.ListSales;
import com.washflow.presentation.protocols.HttpRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListSaleRefundsControllerTest {

  @Mock private ListSales listSales;
  @Mock private SaleRefund refund;

  @Test
  void listsRefundsForTheRequestedDates() {
    when(listSales.refunds(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-10")))
        .thenReturn(List.of(refund));

    var response =
        new ListSaleRefundsController(listSales).handle(query("2026-11-10", "2026-11-10"));

    assertEquals(200, response.statusCode());
    assertEquals(List.of(refund), response.body());
  }

  @Test
  void rejectsMissingOrInvalidDates() {
    ListSaleRefundsController sut = new ListSaleRefundsController(listSales);

    assertEquals(400, sut.handle(query(null, "2026-11-10")).statusCode());
    assertEquals(400, sut.handle(query("ontem", "2026-11-10")).statusCode());
    verifyNoInteractions(listSales);
  }

  private static HttpRequest query(String from, String to) {
    Map<String, String> params = new HashMap<>();
    params.put("from", from);
    params.put("to", to);
    return new HttpRequest(Map.of(), Map.of(), params);
  }
}
