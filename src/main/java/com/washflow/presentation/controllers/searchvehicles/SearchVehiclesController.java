package com.washflow.presentation.controllers.searchvehicles;

import com.washflow.domain.usecases.vehicle.SearchVehicles;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

/** {@code GET /api/operations/vehicles?query=...} - plate prefix, owner name or phone. */
public class SearchVehiclesController implements Controller {

  private final SearchVehicles searchVehicles;

  public SearchVehiclesController(SearchVehicles searchVehicles) {
    this.searchVehicles = searchVehicles;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(searchVehicles.search(request.queryParams().get("query")));
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
