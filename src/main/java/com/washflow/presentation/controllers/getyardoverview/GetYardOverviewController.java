package com.washflow.presentation.controllers.getyardoverview;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.usecases.GetYardOverview;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;

/**
 * Delegates to {@code GetYardOverview} to return the yard overview. Framework-agnostic — depends
 * only on HttpRequest/HttpResponse and the UseCase interface.
 */
public class GetYardOverviewController implements Controller {

  private final GetYardOverview getYardOverview;

  public GetYardOverviewController(GetYardOverview getYardOverview) {
    this.getYardOverview = getYardOverview;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    List<ServiceOrder> yard = getYardOverview.get();
    return new HttpResponse(200, yard);
  }
}
