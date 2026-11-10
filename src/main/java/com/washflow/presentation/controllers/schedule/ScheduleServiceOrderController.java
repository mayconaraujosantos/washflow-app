package com.washflow.presentation.controllers.schedule;

import com.washflow.domain.errors.finance.ServicePriceNotFoundError;
import com.washflow.domain.errors.schedule.InsufficientLeadTimeError;
import com.washflow.domain.errors.schedule.OutsideBusinessHoursError;
import com.washflow.domain.errors.schedule.SlotFullyBookedError;
import com.washflow.domain.errors.vehicle.VehicleNotFoundError;
import com.washflow.domain.usecases.schedule.ScheduleServiceOrder;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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
      int servicePriceId = BodyValues.integer(request.body(), "servicePriceId");
      Instant scheduledAt = Instant.parse(BodyValues.text(request.body(), "scheduledAt"));

      var serviceOrder =
          scheduleServiceOrder.schedule(
              new ScheduleServiceOrder.Params(vehicleId, servicePriceId, scheduledAt));

      return HttpHelper.ok(serviceOrder);
    } catch (DateTimeParseException
        | IllegalArgumentException
        | OutsideBusinessHoursError
        | InsufficientLeadTimeError e) {
      return HttpHelper.badRequest(e);
    } catch (VehicleNotFoundError | ServicePriceNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (SlotFullyBookedError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
