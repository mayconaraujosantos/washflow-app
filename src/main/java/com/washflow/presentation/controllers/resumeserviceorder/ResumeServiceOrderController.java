package com.washflow.presentation.controllers.resumeserviceorder;

import com.washflow.domain.errors.ServiceOrderNotWashingError;
import com.washflow.domain.usecases.ResumeServiceOrder;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

/**
 * Parses the HTTP request for resuming a wash and delegates to {@code ResumeServiceOrder}.
 * Framework-agnostic — depends only on HttpRequest/HttpResponse and the UseCase interface.
 */
public class ResumeServiceOrderController implements Controller {

  private final ResumeServiceOrder resumeServiceOrder;

  public ResumeServiceOrderController(ResumeServiceOrder resumeServiceOrder) {
    this.resumeServiceOrder = resumeServiceOrder;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));

      UUID washerId = (UUID) request.body().get("createdBy");
      if (washerId == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("washerId is required"));
      }

      ResumeServiceOrder.Params params = new ResumeServiceOrder.Params(serviceOrderId, washerId);

      resumeServiceOrder.resume(params);
      return new HttpResponse(200, Map.of("status", "WASHING"));

    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (ServiceOrderNotWashingError e) {
      return new HttpResponse(409, Map.of("error", e.getMessage()));
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
