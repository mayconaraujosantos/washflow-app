package com.washflow.presentation.controllers.serviceorder;

import com.washflow.domain.entities.shared.PaymentMethod;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotRefundableError;
import com.washflow.domain.usecases.serviceorder.RefundServiceOrder;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Parses the manager's refund request and delegates to {@code RefundServiceOrder}. */
public class RefundServiceOrderController implements Controller {

  private final RefundServiceOrder refundServiceOrder;

  public RefundServiceOrderController(RefundServiceOrder refundServiceOrder) {
    this.refundServiceOrder = refundServiceOrder;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));
      Map<String, Object> body = request.body();
      UUID managerId = (UUID) body.get("createdBy");
      if (managerId == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("managerId is required"));
      }
      String method = (String) body.get("refundMethod");
      PaymentMethod refundMethod =
          method == null || method.isBlank()
              ? null
              : PaymentMethod.valueOf(method.trim().toUpperCase(Locale.ROOT));
      return HttpHelper.ok(
          refundServiceOrder.refund(
              new RefundServiceOrder.Params(
                  serviceOrderId,
                  managerId,
                  (String) body.get("reason"),
                  (String) body.get("description"),
                  refundMethod,
                  Boolean.TRUE.equals(body.get("reverseCommission")))));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (ServiceOrderNotRefundableError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
