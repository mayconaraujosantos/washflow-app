package com.washflow.presentation.controllers.deletecashtransaction;

import com.washflow.domain.usecases.DeleteCashTransaction;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

public class DeleteCashTransactionController implements Controller {

  private final DeleteCashTransaction deleteCashTransaction;

  public DeleteCashTransactionController(DeleteCashTransaction deleteCashTransaction) {
    this.deleteCashTransaction = deleteCashTransaction;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID id = UUID.fromString(request.pathParams().get("id"));
      deleteCashTransaction.delete(id);
      return new HttpResponse(204, "");
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
