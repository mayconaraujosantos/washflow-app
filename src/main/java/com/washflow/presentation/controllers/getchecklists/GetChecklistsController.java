package com.washflow.presentation.controllers.getchecklists;

import com.washflow.domain.entities.ServiceOrderChecklist;
import com.washflow.domain.usecases.ListServiceOrderChecklists;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;
import java.util.UUID;

/**
 * Parses the service order ID from the path and delegates to {@code ListServiceOrderChecklists}.
 * Framework-agnostic — depends only on HttpRequest/HttpResponse and the UseCase interface.
 */
public class GetChecklistsController implements Controller {

  private final ListServiceOrderChecklists listServiceOrderChecklists;

  public GetChecklistsController(ListServiceOrderChecklists listServiceOrderChecklists) {
    this.listServiceOrderChecklists = listServiceOrderChecklists;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));

      List<ServiceOrderChecklist> checklists = listServiceOrderChecklists.list(serviceOrderId);

      return HttpHelper.ok(checklists);

    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
