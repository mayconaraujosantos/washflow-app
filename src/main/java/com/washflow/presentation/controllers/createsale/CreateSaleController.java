package com.washflow.presentation.controllers.createsale;

import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.SaleType;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.errors.VehicleNotFoundError;
import com.washflow.domain.usecases.CreateSale;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
                  uuid(body.get("vehicleId")),
                  text(body.get("customerName")),
                  text(body.get("customerPhone")),
                  list(body.get("items")).stream()
                      .map(
                          item ->
                              new CreateSale.Item(
                                  number(item.get("servicePriceId")).intValue(),
                                  number(item.get("quantity")).intValue()))
                      .toList(),
                  body.get("discount") == null ? null : decimal(body.get("discount")),
                  list(body.get("payments")).stream()
                      .map(
                          payment ->
                              new CreateSale.Payment(
                                  PaymentMethod.valueOf(
                                      text(payment.get("method")).toUpperCase(Locale.ROOT)),
                                  decimal(payment.get("amount"))))
                      .toList(),
                  text(body.get("notes")),
                  (UUID) body.get("createdBy")));
      return HttpHelper.ok(sale);
    } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
      return HttpHelper.badRequest(
          e instanceof IllegalArgumentException
              ? e
              : new IllegalArgumentException("Dados da venda inválidos"));
    } catch (ServicePriceNotFoundError | VehicleNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }

  private static String required(Map<String, Object> body, String key) {
    String value = text(body.get(key));
    if (value == null) {
      throw new IllegalArgumentException(key + " é obrigatório");
    }
    return value;
  }

  private static String text(Object value) {
    return value == null ? null : value.toString();
  }

  private static UUID uuid(Object value) {
    String text = text(value);
    return text == null || text.isBlank() ? null : UUID.fromString(text);
  }

  private static Number number(Object value) {
    if (value instanceof Number n) {
      return n;
    }
    throw new IllegalArgumentException("Número inválido: " + value);
  }

  private static BigDecimal decimal(Object value) {
    if (value == null) {
      throw new IllegalArgumentException("Valor obrigatório");
    }
    return new BigDecimal(value.toString());
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> list(Object value) {
    return value == null ? List.of() : (List<Map<String, Object>>) value;
  }
}
