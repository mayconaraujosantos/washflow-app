package com.washflow.presentation.controllers.finance;

import com.washflow.domain.entities.finance.OperationsReport;
import com.washflow.domain.usecases.finance.GetOperationsReport;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Parses the date range query params and delegates to {@code GetOperationsReport}.
 * Framework-agnostic — depends only on HttpRequest/HttpResponse and the UseCase interface.
 */
public class GetOperationsReportController implements Controller {

  private final GetOperationsReport getOperationsReport;

  public GetOperationsReportController(GetOperationsReport getOperationsReport) {
    this.getOperationsReport = getOperationsReport;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      String fromParam = request.queryParams().get("from");
      String toParam = request.queryParams().get("to");
      if (fromParam == null || toParam == null) {
        return HttpHelper.badRequest(
            new IllegalArgumentException("Provide from and to as YYYY-MM-DD dates"));
      }

      GetOperationsReport.Params params =
          new GetOperationsReport.Params(LocalDate.parse(fromParam), LocalDate.parse(toParam));
      OperationsReport report = getOperationsReport.get(params);
      return new HttpResponse(200, report);

    } catch (DateTimeParseException e) {
      return HttpHelper.badRequest(
          new IllegalArgumentException("Provide from and to as YYYY-MM-DD dates"));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
