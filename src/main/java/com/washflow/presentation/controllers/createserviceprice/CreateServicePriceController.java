package com.washflow.presentation.controllers.createserviceprice;

import com.washflow.domain.usecases.CreateServicePrice;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.util.Map;

/** {@code POST /api/services}: {@code name}, {@code price}, optional {@code washerCommission}. */
public class CreateServicePriceController implements Controller {

  private final CreateServicePrice createServicePrice;

  public CreateServicePriceController(CreateServicePrice createServicePrice) {
    this.createServicePrice = createServicePrice;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      return HttpHelper.ok(
          createServicePrice.create(
              new CreateServicePrice.Params(
                  body.get("name") == null ? null : body.get("name").toString(),
                  decimal(body.get("price")),
                  decimal(body.get("washerCommission")))));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }

  private static BigDecimal decimal(Object value) {
    return value == null ? null : new BigDecimal(value.toString());
  }
}
