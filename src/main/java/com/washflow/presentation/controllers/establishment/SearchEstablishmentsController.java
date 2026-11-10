package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/**
 * {@code GET /api/establishments?query=} (public): active lava-jatos by name, for the customer
 * picker.
 */
public class SearchEstablishmentsController implements Controller {

  private final ManagePlatform platform;

  public SearchEstablishmentsController(ManagePlatform platform) {
    this.platform = platform;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, String> query = request.queryParams() == null ? Map.of() : request.queryParams();
      return HttpHelper.ok(platform.search(query.get("query")));
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
