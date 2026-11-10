package com.washflow.presentation.controllers.sale;

import com.washflow.domain.entities.sale.SaleType;
import com.washflow.domain.entities.shared.PaymentMethod;
import com.washflow.domain.errors.finance.ServicePriceNotFoundError;
import com.washflow.domain.errors.vehicle.VehicleNotFoundError;
import com.washflow.domain.usecases.sale.CreateSale;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * {@code POST /api/sales}: {@code type} (QUICK_SALE|BUDGET), {@code items} [{servicePriceId,
 * quantity}], optional {@code discount}, {@code payments} [{method, amount}], {@code vehicleId},
 * {@code customerName}, {@code customerPhone} and {@code notes}.
 */
public class CreateSaleController implements Controller {

  private final CreateSale createSale;

  public CreateSaleController(CreateSale createSale) {
    this.createSale = createSale;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      var sale =
          createSale.create(
              new CreateSale.Params(
                  SaleType.valueOf(required(body, "type").toUpperCase(Locale.ROOT)),
                  BodyValues.uuid(body, "vehicleId"),
                  BodyValues.text(body, "customerName"),
                  BodyValues.text(body, "customerPhone"),
                  BodyValues.objects(body, "items").stream()
                      .map(
                          item ->
                              new CreateSale.Item(
                                  BodyValues.integer(item, "servicePriceId"),
                                  BodyValues.integer(item, "quantity")))
                      .toList(),
                  BodyValues.decimal(body, "discount"),
                  BodyValues.objects(body, "payments").stream()
                      .map(
                          payment ->
                              new CreateSale.Payment(
                                  PaymentMethod.valueOf(
                                      required(payment, "method").toUpperCase(Locale.ROOT)),
                                  requiredDecimal(payment, "amount")))
                      .toList(),
                  BodyValues.text(body, "notes"),
                  BodyValues.createdBy(body)));
      return HttpHelper.ok(sale);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (ServicePriceNotFoundError | VehicleNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }

  private static String required(Map<String, Object> body, String key) {
    String value = BodyValues.text(body, key);
    if (value == null) {
      throw new IllegalArgumentException(key + " é obrigatório");
    }
    return value;
  }

  private static BigDecimal requiredDecimal(Map<String, Object> body, String key) {
    BigDecimal value = BodyValues.decimal(body, key);
    if (value == null) {
      throw new IllegalArgumentException(key + " é obrigatório");
    }
    return value;
  }
}
