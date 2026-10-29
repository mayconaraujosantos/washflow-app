package com.washflow.presentation.controllers.createvehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.errors.VehiclePlateAlreadyRegisteredError;
import com.washflow.domain.usecases.CreateVehicle;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateVehicleControllerTest {

  @Mock private CreateVehicle createVehicle;
  @Mock private Validation validation;

  private CreateVehicleController sut;
  private UUID customerId;

  @BeforeEach
  void setUp() {
    sut = new CreateVehicleController(createVehicle, validation);
    customerId = UUID.randomUUID();
  }

  @Test
  void createsVehicleAndReturnsOk() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("customerId", customerId.toString());
    body.put("plate", "ABC1D23");
    body.put("model", "Honda Civic");
    body.put("color", "Prata");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    Vehicle saved = new Vehicle(UUID.randomUUID(), customerId, "ABC1D23", "Honda Civic", "Prata");
    when(createVehicle.create(any(CreateVehicle.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(createVehicle).create(any(CreateVehicle.Params.class));
  }

  @Test
  void returnsBadRequestWhenValidationFails() {
    Map<String, Object> body = new HashMap<>();
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(new IllegalArgumentException("missing field"));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsNotFoundWhenUserNotFound() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("customerId", customerId.toString());
    body.put("plate", "ABC1D23");
    body.put("model", "Honda Civic");
    body.put("color", "Prata");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(createVehicle.create(any(CreateVehicle.Params.class)))
        .thenThrow(new UserNotFoundError(customerId));

    HttpResponse response = sut.handle(request);

    assertEquals(404, response.statusCode());
  }

  @Test
  void returnsConflictWhenPlateAlreadyRegistered() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("customerId", customerId.toString());
    body.put("plate", "ABC1D23");
    body.put("model", "Honda Civic");
    body.put("color", "Prata");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(createVehicle.create(any(CreateVehicle.Params.class)))
        .thenThrow(new VehiclePlateAlreadyRegisteredError("ABC1D23"));

    HttpResponse response = sut.handle(request);

    assertEquals(409, response.statusCode());
  }
}
