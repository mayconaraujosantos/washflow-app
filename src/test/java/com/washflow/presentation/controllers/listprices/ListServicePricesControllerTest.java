package com.washflow.presentation.controllers.listserviceprices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServicePrice;
import com.washflow.domain.usecases.ListServicePrices;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListServicePricesControllerTest {

  @Mock private ListServicePrices listServicePrices;

  private ListServicePricesController sut;

  @BeforeEach
  void setUp() {
    sut = new ListServicePricesController(listServicePrices);
  }

  @Test
  void listsServicePricesAndReturnsOk() throws Exception {
    List<ServicePrice> prices =
        List.of(
            new ServicePrice(1, "Lavagem Simples", new BigDecimal("35.00"), new BigDecimal("5.00")),
            new ServicePrice(
                2, "Lavagem Completa", new BigDecimal("60.00"), new BigDecimal("10.00")));
    when(listServicePrices.list()).thenReturn(prices);

    HttpRequest request = new HttpRequest(Map.of(), Map.of(), Map.of());
    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(listServicePrices).list();
  }

  @Test
  void returnsEmptyListWhenNoPrices() throws Exception {
    when(listServicePrices.list()).thenReturn(List.of());

    HttpRequest request = new HttpRequest(Map.of(), Map.of(), Map.of());
    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
  }
}
