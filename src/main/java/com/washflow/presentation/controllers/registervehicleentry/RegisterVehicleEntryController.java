package com.washflow.presentation.controllers.registervehicleentry;

import com.washflow.domain.errors.cash.ServicePriceNotFoundError;
import com.washflow.domain.errors.schedule.InsufficientLeadTimeError;
import com.washflow.domain.errors.schedule.OutsideBusinessHoursError;
import com.washflow.domain.errors.schedule.SlotFullyBookedError;
import com.washflow.domain.errors.vehicle.InvalidPlateError;
import com.washflow.domain.errors.vehicle.VehicleAlreadyInYardError;
import com.washflow.domain.usecases.yard.RegisterVehicleEntry;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * {@code POST /api/operations/entries}: body {@code plate}, {@code servicePriceId} (required) and
 * optional {@code vehicleModel}, {@code vehicleColor}, {@code customerName}, {@code customerPhone},
 * {@code scheduledAt}, {@code expectedDeliveryAt} (ISO-8601 instants) and {@code notes}.
 */
public class RegisterVehicleEntryController implements Controller {

  private final RegisterVehicleEntry registerVehicleEntry;

  public RegisterVehicleEntryController(RegisterVehicleEntry registerVehicleEntry) {
    this.registerVehicleEntry = registerVehicleEntry;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      if (!(body.get("servicePriceId") instanceof Number servicePriceId)) {
        return HttpHelper.badRequest(new IllegalArgumentException("Escolha o serviço"));
      }

      var serviceOrder =
          registerVehicleEntry.register(
              new RegisterVehicleEntry.Params(
                  text(body, "plate"),
                  text(body, "vehicleModel"),
                  text(body, "vehicleColor"),
                  text(body, "customerName"),
                  text(body, "customerPhone"),
                  servicePriceId.intValue(),
                  instant(body, "scheduledAt"),
                  instant(body, "expectedDeliveryAt"),
                  text(body, "notes")));

      return HttpHelper.ok(serviceOrder);
    } catch (DateTimeParseException e) {
      return HttpHelper.badRequest(new IllegalArgumentException("Data/hora inválida"));
    } catch (IllegalArgumentException
        | InvalidPlateError
        | OutsideBusinessHoursError
        | InsufficientLeadTimeError e) {
      return HttpHelper.badRequest(e);
    } catch (ServicePriceNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (SlotFullyBookedError | VehicleAlreadyInYardError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }

  private static String text(Map<String, Object> body, String key) {
    Object value = body.get(key);
    return value == null ? null : value.toString();
  }

  private static Instant instant(Map<String, Object> body, String key) {
    String value = text(body, key);
    return value == null || value.isBlank() ? null : Instant.parse(value);
  }
}
