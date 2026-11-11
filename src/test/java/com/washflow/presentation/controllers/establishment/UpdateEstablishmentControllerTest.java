package com.washflow.presentation.controllers.establishment;

import static com.washflow.presentation.helpers.HttpRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.washflow.domain.usecases.establishment.ManageEstablishment;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UpdateEstablishmentControllerTest {

  @Test
  void updatesNameHoursSundayAndCapacity() {
    ManageEstablishment manage = mock(ManageEstablishment.class);

    HttpResponse response =
        new UpdateEstablishmentController(manage)
            .handle(
                request(
                    Map.of(),
                    "name",
                    "Lava 24h",
                    "openingHour",
                    6,
                    "closingHour",
                    22,
                    "openOnSunday",
                    true,
                    "maxVehiclesPerHour",
                    4));

    assertEquals(200, response.statusCode());
    verify(manage).update(new ManageEstablishment.Params("Lava 24h", 6, 22, true, 4));
  }

  @Test
  void sundayIsOptionalButMustBeABoolean() {
    ManageEstablishment manage = mock(ManageEstablishment.class);
    UpdateEstablishmentController sut = new UpdateEstablishmentController(manage);

    sut.handle(
        request(
            Map.of(),
            "name",
            "Lava",
            "openingHour",
            8,
            "closingHour",
            18,
            "maxVehiclesPerHour",
            3));
    verify(manage).update(new ManageEstablishment.Params("Lava", 8, 18, false, 3));

    assertEquals(
        400,
        sut.handle(
                request(
                    Map.of(),
                    "name",
                    "Lava",
                    "openingHour",
                    8,
                    "closingHour",
                    18,
                    "openOnSunday",
                    "sim",
                    "maxVehiclesPerHour",
                    3))
            .statusCode());
  }
}
