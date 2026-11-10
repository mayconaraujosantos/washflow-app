package com.washflow.presentation.controllers.sale;

import com.washflow.domain.entities.sale.SaleType;
import com.washflow.domain.usecases.sale.ListSales;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** {@code GET /api/sales?from=YYYY-MM-DD&to=YYYY-MM-DD[&type=QUICK_SALE|BUDGET]}. */
public class ListSalesController implements Controller {

  private static final String INVALID_RANGE = "Provide from and to as YYYY-MM-DD dates";

  private final ListSales listSales;

  public ListSalesController(ListSales listSales) {
    this.listSales = listSales;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      String from = request.queryParams().get("from");
      String to = request.queryParams().get("to");
      if (from == null || to == null) {
        return HttpHelper.badRequest(new IllegalArgumentException(INVALID_RANGE));
      }
      String type = request.queryParams().get("type");
      return HttpHelper.ok(
          listSales.list(
              new ListSales.Params(
                  type == null || type.isBlank()
                      ? null
                      : SaleType.valueOf(type.toUpperCase(Locale.ROOT)),
                  LocalDate.parse(from),
                  LocalDate.parse(to))));
    } catch (DateTimeParseException e) {
      return HttpHelper.badRequest(new IllegalArgumentException(INVALID_RANGE));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
