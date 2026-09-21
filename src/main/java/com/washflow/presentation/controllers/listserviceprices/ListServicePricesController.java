package com.washflow.presentation.controllers.listserviceprices;

import com.washflow.domain.usecases.ListServicePrices;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

public class ListServicePricesController implements Controller {

  private final ListServicePrices listServicePrices;

  public ListServicePricesController(ListServicePrices listServicePrices) {
    this.listServicePrices = listServicePrices;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(listServicePrices.list());
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
