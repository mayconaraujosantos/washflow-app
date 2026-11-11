package com.washflow.presentation.controllers.yard;

import static com.washflow.presentation.helpers.HttpRequests.body;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.domain.errors.finance.ServicePriceNotFoundError;
import com.washflow.domain.errors.schedule.SlotFullyBookedError;
import com.washflow.domain.errors.vehicle.VehicleAlreadyInYardError;
import com.washflow.domain.usecases.yard.RegisterVehicleEntry;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RegisterVehicleEntryControllerTest {

  @Test
  void registersAWalkInOrScheduledEntry() throws Exception {
    RegisterVehicleEntry register = mock(RegisterVehicleEntry.class);

    HttpResponse response =
        new RegisterVehicleEntryController(register)
            .handle(
                body(
                    "plate",
                    "ABC1D23",
                    "servicePriceId",
                    1,
                    "customerName",
                    "Ana",
                    "scheduledAt",
                    "2026-11-11T14:00:00Z",
                    "notes",
                    "Rodas"));

    assertEquals(200, response.statusCode());
    verify(register)
        .register(
            new RegisterVehicleEntry.Params(
                "ABC1D23",
                null,
                null,
                "Ana",
                null,
                1,
                Instant.parse("2026-11-11T14:00:00Z"),
                null,
                "Rodas"));
  }

  @Test
  void rejectsMissingServiceInvalidDatesAndWrongTypes() {
    RegisterVehicleEntry register = mock(RegisterVehicleEntry.class);
    RegisterVehicleEntryController sut = new RegisterVehicleEntryController(register);

    assertEquals(400, sut.handle(body("plate", "ABC1D23")).statusCode());
    assertEquals(400, sut.handle(body("servicePriceId", 1, "scheduledAt", "amanha")).statusCode());
    assertEquals(400, sut.handle(body("servicePriceId", 1, "customerName", 5)).statusCode());
    verifyNoInteractions(register);
  }

  @Test
  void mapsUnknownServiceFullSlotAndCarAlreadyInTheYard() throws Exception {
    RegisterVehicleEntry register = mock(RegisterVehicleEntry.class);
    when(register.register(any()))
        .thenThrow(new ServicePriceNotFoundError(9))
        .thenThrow(new SlotFullyBookedError(Instant.now(), 3))
        .thenThrow(new VehicleAlreadyInYardError("ABC1D23"));
    RegisterVehicleEntryController sut = new RegisterVehicleEntryController(register);

    assertEquals(404, sut.handle(body("servicePriceId", 9)).statusCode());
    assertEquals(409, sut.handle(body("servicePriceId", 1)).statusCode());
    assertEquals(409, sut.handle(body("servicePriceId", 1)).statusCode());
  }
}
