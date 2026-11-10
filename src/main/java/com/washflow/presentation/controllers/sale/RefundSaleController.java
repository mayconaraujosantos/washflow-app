package com.washflow.presentation.controllers.sale;

import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.errors.sale.SaleNotFoundError;
import com.washflow.domain.errors.sale.SaleRefundNotAllowedError;
import com.washflow.domain.usecases.sale.RefundSale;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** {@code POST /api/sales/{id}/refunds} with {@code method}, {@code amount}, {@code reason}. */
public class RefundSaleController implements Controller {

  private final RefundSale refundSale;

  public RefundSaleController(RefundSale refundSale) {
    this.refundSale = refundSale;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID saleId = UUID.fromString(request.pathParams().get("id"));
      Map<String, Object> body = request.body();
      UUID managerId = (UUID) body.get("createdBy");
      if (managerId == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("managerId is required"));
      }
      Object method = body.get("method");
      Object amount = body.get("amount");
      return HttpHelper.ok(
          refundSale.refund(
              new RefundSale.Params(
                  saleId,
                  managerId,
                  method == null
                      ? null
                      : PaymentMethod.valueOf(method.toString().trim().toUpperCase(Locale.ROOT)),
                  amount == null ? null : new BigDecimal(amount.toString()),
                  (String) body.get("reason"),
                  (String) body.get("description"))));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (SaleNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (SaleRefundNotAllowedError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
