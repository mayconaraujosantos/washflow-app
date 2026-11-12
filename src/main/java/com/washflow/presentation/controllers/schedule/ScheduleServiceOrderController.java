package com.washflow.presentation.controllers.schedule;

import com.washflow.domain.usecases.schedule.ScheduleServiceOrder;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

public class ScheduleServiceOrderController implements Controller {

  private final ScheduleServiceOrder scheduleServiceOrder;
  private final Validation validation;

  public ScheduleServiceOrderController(
      ScheduleServiceOrder scheduleServiceOrder, Validation validation) {
    this.scheduleServiceOrder = scheduleServiceOrder;
    this.validation = validation;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Exception validationError = validation.validate(request.body());
      if (validationError != null) {
        return HttpHelper.badRequest(validationError);
      }

      UUID vehicleId = UUID.fromString(BodyValues.text(request.body(), "vehicleId"));
      List<Integer> servicePriceIds = BodyValues.integers(request.body(), "servicePriceIds");
      if (servicePriceIds.isEmpty()) {
        servicePriceIds = List.of(BodyValues.integer(request.body(), "servicePriceId"));
      }
      Instant scheduledAt = Instant.parse(BodyValues.text(request.body(), "scheduledAt"));

      var serviceOrder =
          scheduleServiceOrder.schedule(
              new ScheduleServiceOrder.Params(
                  vehicleId, servicePriceIds.getFirst(), servicePriceIds, scheduledAt));

      return HttpHelper.ok(serviceOrder);
    } catch (DateTimeParseException | IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
