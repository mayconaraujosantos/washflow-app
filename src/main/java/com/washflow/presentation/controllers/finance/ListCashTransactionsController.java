package com.washflow.presentation.controllers.finance;

import com.washflow.domain.entities.finance.CashTransaction;
import com.washflow.domain.usecases.finance.ListCashTransactions;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ListCashTransactionsController implements Controller {

  private final ListCashTransactions listCashTransactions;

  public ListCashTransactionsController(ListCashTransactions listCashTransactions) {
    this.listCashTransactions = listCashTransactions;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      // GET requests carry the range in the query string; the body is kept as a fallback.
      String fromStr = param(request, "from");
      String toStr = param(request, "to");

      List<CashTransaction> transactions;
      if (fromStr != null && toStr != null) {
        LocalDate from = LocalDate.parse(fromStr);
        LocalDate to = LocalDate.parse(toStr);
        transactions = listCashTransactions.list(from, to);
      } else {
        transactions = listCashTransactions.listAll();
      }
      return HttpHelper.ok(transactions);
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
