package com.washflow.presentation.controllers.finance;

import com.washflow.domain.usecases.finance.GetCashTransactionReport;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class GetCashTransactionReportController implements Controller {

  private final GetCashTransactionReport getCashTransactionReport;

  public GetCashTransactionReportController(GetCashTransactionReport getCashTransactionReport) {
    this.getCashTransactionReport = getCashTransactionReport;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      // GET requests carry the range in the query string; the body is kept as a fallback.
      String fromStr = param(request, "from");
      String toStr = param(request, "to");

      if (fromStr == null || toStr == null) {
        return HttpHelper.badRequest(
            new IllegalArgumentException("Provide from and to as YYYY-MM-DD dates"));
      }

      LocalDate from = LocalDate.parse(fromStr);
      LocalDate to = LocalDate.parse(toStr);

      if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 365) {
        return HttpHelper.badRequest(
            new IllegalArgumentException(
                "Report range must be ordered and no longer than 366 days"));
      }

      Map<String, Object> report = getCashTransactionReport.report(from, to);
      return HttpHelper.ok(report);

    } catch (Exception e) {
      return HttpHelper.badRequest(e);
    }
  }

  private static String param(HttpRequest request, String name) {
    String value = request.queryParams() == null ? null : request.queryParams().get(name);
    if (value != null && !value.isBlank()) {
      return value;
    }
    Map<String, Object> body = request.body();
    Object fromBody = body == null ? null : body.get(name);
    return fromBody == null ? null : fromBody.toString();
  }
}
