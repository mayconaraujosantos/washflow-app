package com.washflow.presentation.controllers.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.washflow.domain.usecases.pricing.CreateServicePrice;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateServicePriceControllerTest {

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
  void createsACatalogItemWithPriceAndCommission() {
    CreateServicePrice create = mock(CreateServicePrice.class);

    HttpResponse response =
        new CreateServicePriceController(create)
            .handle(body("name", "Cera", "price", 15.5, "washerCommission", 2));

    assertEquals(200, response.statusCode());
    verify(create)
        .create(new CreateServicePrice.Params("Cera", new BigDecimal("15.5"), new BigDecimal("2")));
  }

  @Test
  void pricesMustBeNumbers() {
    CreateServicePrice create = mock(CreateServicePrice.class);

    assertEquals(
        400,
        new CreateServicePriceController(create)
            .handle(body("name", "Cera", "price", "15.5"))
            .statusCode());
    verifyNoInteractions(create);
  }
}
