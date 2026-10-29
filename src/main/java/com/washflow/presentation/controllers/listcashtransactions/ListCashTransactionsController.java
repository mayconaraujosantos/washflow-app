package com.washflow.presentation.controllers.listcashtransactions;

import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.usecases.ListCashTransactions;
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
      Map<String, Object> body = request.body();
      String fromStr = (String) body.get("from");
      String toStr = (String) body.get("to");

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
}
