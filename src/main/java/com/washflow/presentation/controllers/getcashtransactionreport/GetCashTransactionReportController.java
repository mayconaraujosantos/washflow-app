package com.washflow.presentation.controllers.getcashtransactionreport;

import com.washflow.domain.usecases.GetCashTransactionReport;
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
      Map<String, Object> body = request.body();
      String fromStr = (String) body.get("from");
      String toStr = (String) body.get("to");

      if (fromStr == null || toStr == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("Provide from and to as YYYY-MM-DD dates"));
      }

      LocalDate from = LocalDate.parse(fromStr);
      LocalDate to = LocalDate.parse(toStr);

      if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 365) {
        return HttpHelper.badRequest(new IllegalArgumentException("Report range must be ordered and no longer than 366 days"));
      }

      Map<String, Object> report = getCashTransactionReport.report(from, to);
      return HttpHelper.ok(report);

    } catch (Exception e) {
      return HttpHelper.badRequest(e);
    }
  }
}
