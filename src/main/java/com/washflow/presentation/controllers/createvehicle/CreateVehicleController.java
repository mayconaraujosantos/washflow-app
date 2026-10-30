package com.washflow.presentation.controllers.createvehicle;

import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.errors.VehiclePlateAlreadyRegisteredError;
import com.washflow.domain.usecases.CreateVehicle;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
import java.util.UUID;

public class CreateVehicleController implements Controller {

  private final CreateVehicle createVehicle;
  private final Validation validation;

  public CreateVehicleController(CreateVehicle createVehicle, Validation validation) {
    this.createVehicle = createVehicle;
    this.validation = validation;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Exception validationError = validation.validate(request.body());
      if (validationError != null) {
        return HttpHelper.badRequest(validationError);
      }

      UUID customerId = UUID.fromString((String) request.body().get("customerId"));
      String plate = (String) request.body().get("plate");
      String model = (String) request.body().get("model");
      String color = (String) request.body().get("color");

      var vehicle = createVehicle.create(new CreateVehicle.Params(customerId, plate, model, color));

      return HttpHelper.ok(vehicle);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (UserNotFoundError e) {
      return HttpHelper.notFound(e);
    } catch (VehiclePlateAlreadyRegisteredError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
