package com.washflow.presentation.controllers.updatecashtransaction;

import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.CashTransaction.TransactionType;
import com.washflow.domain.usecases.UpdateCashTransaction;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class UpdateCashTransactionController implements Controller {

  private final UpdateCashTransaction updateCashTransaction;

  public UpdateCashTransactionController(UpdateCashTransaction updateCashTransaction) {
    this.updateCashTransaction = updateCashTransaction;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID id = UUID.fromString(request.pathParams().get("id"));
      Map<String, Object> body = request.body();

      TransactionType type = null;
      Object typeObj = body.get("type");
      if (typeObj != null) {
        type = TransactionType.valueOf((String) typeObj);
      }

      BigDecimal amount = null;
      Object amountObj = body.get("amount");
      if (amountObj != null) {
        amount = new BigDecimal((String) amountObj);
      }

      UUID categoryId = null;
      Object catIdObj = body.get("categoryId");
      if (catIdObj != null) {
        categoryId = UUID.fromString((String) catIdObj);
      }

      String description = (String) body.get("description");

      LocalDate referenceDate = null;
      Object dateObj = body.get("referenceDate");
      if (dateObj != null) {
        referenceDate = LocalDate.parse((String) dateObj);
      }

      UUID serviceOrderId = null;
      Object soIdObj = body.get("serviceOrderId");
      if (soIdObj != null) {
        serviceOrderId = UUID.fromString((String) soIdObj);
      }

      CashTransaction.PartialUpdate update =
          new CashTransaction.PartialUpdate(
              type, amount, categoryId, description, referenceDate, serviceOrderId);

      var updated = updateCashTransaction.update(id, update);
      return HttpHelper.ok(updated);

    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
