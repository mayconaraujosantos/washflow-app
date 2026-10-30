package com.washflow.presentation.controllers.health;

import com.washflow.data.protocols.db.CheckDatabaseHealthRepository;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.LinkedHashMap;

/**
 * A liveness check isn't domain business logic, so unlike {@code ScheduleServiceOrderController}
 * this depends directly on the {@code data.protocols.db} port instead of a {@code domain.usecases}
 * interface - there's no business rule here to express as a use case.
 */
public class HealthController implements Controller {

  private final CheckDatabaseHealthRepository checkDatabaseHealthRepository;

  public HealthController(CheckDatabaseHealthRepository checkDatabaseHealthRepository) {
    this.checkDatabaseHealthRepository = checkDatabaseHealthRepository;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    var body = new LinkedHashMap<String, String>();
    body.put("status", "ok");
    body.put("service", "washflow-api");
    body.put("database", checkDatabaseHealthRepository.isUp() ? "up" : "down");
    return HttpHelper.ok(body);
  }
}
