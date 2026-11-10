package com.washflow.presentation.controllers.schedule;

import com.washflow.domain.usecases.schedule.ScheduleAvailability;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** {@code GET /api/schedule/availability?date=YYYY-MM-DD} (public: counts only, no orders). */
public class ScheduleAvailabilityController implements Controller {

  private final ScheduleAvailability availability;

  public ScheduleAvailabilityController(ScheduleAvailability availability) {
    this.availability = availability;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      String date = request.queryParams().get("date");
      if (date == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("Informe a data (YYYY-MM-DD)"));
      }
      return HttpHelper.ok(availability.forDate(LocalDate.parse(date)));
    } catch (DateTimeParseException e) {
      return HttpHelper.badRequest(new IllegalArgumentException("Informe a data (YYYY-MM-DD)"));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
