package com.washflow.presentation.controllers.createcashtransaction;

import com.washflow.domain.entities.CashTransaction.TransactionType;
import com.washflow.domain.errors.InvalidTransactionTypeError;
import com.washflow.domain.usecases.CreateCashTransaction;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class CreateCashTransactionController implements Controller {

  private final CreateCashTransaction createCashTransaction;

  public CreateCashTransactionController(CreateCashTransaction createCashTransaction) {
    this.createCashTransaction = createCashTransaction;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body();

      String typeStr = (String) body.get("type");
      if (typeStr == null || typeStr.isBlank()) {
        return HttpHelper.badRequest(new IllegalArgumentException("type is required"));
      }
      TransactionType type = TransactionType.valueOf(typeStr);

      String amountStr = (String) body.get("amount");
      if (amountStr == null || amountStr.isBlank()) {
        return HttpHelper.badRequest(new IllegalArgumentException("amount is required"));
      }
      BigDecimal amount = new BigDecimal(amountStr);

      String dateStr = (String) body.get("referenceDate");
      if (dateStr == null || dateStr.isBlank()) {
        return HttpHelper.badRequest(new IllegalArgumentException("referenceDate is required"));
      }
      LocalDate referenceDate = LocalDate.parse(dateStr);

      UUID categoryId = null;
      Object catId = body.get("categoryId");
      if (catId != null) {
        categoryId = UUID.fromString((String) catId);
      }

      String description = (String) body.get("description");

      UUID serviceOrderId = null;
      Object soId = body.get("serviceOrderId");
      if (soId != null) {
        serviceOrderId = UUID.fromString((String) soId);
      }

      UUID createdBy = (UUID) body.get("createdBy");
      if (createdBy == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("createdBy is required"));
      }

      CreateCashTransaction.Params params =
          new CreateCashTransaction.Params(
              type, amount, categoryId, description, referenceDate, serviceOrderId, createdBy);

      var saved = createCashTransaction.create(params);
      return new HttpResponse(201, saved);

    } catch (IllegalArgumentException | InvalidTransactionTypeError e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
