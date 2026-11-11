package com.washflow.presentation.controllers.yard;

import com.washflow.domain.usecases.yard.RegisterVehicleEntry;
import com.washflow.presentation.helpers.BodyValues;
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
                  BodyValues.text(body, "plate"),
                  BodyValues.text(body, "vehicleModel"),
                  BodyValues.text(body, "vehicleColor"),
                  BodyValues.text(body, "customerName"),
                  BodyValues.text(body, "customerPhone"),
                  servicePriceId.intValue(),
                  instant(body, "scheduledAt"),
                  instant(body, "expectedDeliveryAt"),
                  BodyValues.text(body, "notes")));

      return HttpHelper.ok(serviceOrder);
    } catch (DateTimeParseException e) {
      return HttpHelper.badRequest(new IllegalArgumentException("Data/hora inválida"));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }

  private static Instant instant(Map<String, Object> body, String key) {
    String value = BodyValues.text(body, key);
    return value == null || value.isBlank() ? null : Instant.parse(value);
  }
}
