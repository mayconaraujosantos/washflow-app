package com.washflow.presentation.controllers.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.errors.establishment.EstablishmentSlugTakenError;
import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateEstablishmentControllerTest {

  private static HttpRequest request(Map<String, String> path, Object... pairs) {
    Map<String, Object> body = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      body.put((String) pairs[i], pairs[i + 1]);
    }
    return new HttpRequest(body, path, Map.of());
  }

  @Test
  void createsTheLavaJatoWithItsFirstManager() throws Exception {
    ManagePlatform platform = mock(ManagePlatform.class);

    HttpResponse response =
        new CreateEstablishmentController(platform)
            .handle(
                request(
                    Map.of(),
                    "name",
                    "Lava",
                    "slug",
                    "lava",
                    "openingHour",
                    8,
                    "closingHour",
                    18,
                    "maxVehiclesPerHour",
                    3,
                    "managerName",
                    "Ana",
                    "managerEmail",
                    "ana@x.test",
                    "managerPhone",
                    "92999990001",
                    "managerPassword",
                    "secret-123"));

    assertEquals(200, response.statusCode());
    verify(platform)
        .create(
            new ManagePlatform.NewEstablishment(
                "Lava", "lava", 8, 18, false, 3, "Ana", "ana@x.test", "92999990001", "secret-123"));
  }

  @Test
  void mapsATakenSlugAndWrongTypes() throws Exception {
    ManagePlatform platform = mock(ManagePlatform.class);
    when(platform.create(any())).thenThrow(new EstablishmentSlugTakenError("lava"));
    CreateEstablishmentController sut = new CreateEstablishmentController(platform);

    assertEquals(
        409,
        sut.handle(request(Map.of(), "openingHour", 8, "closingHour", 18, "maxVehiclesPerHour", 3))
            .statusCode());
    assertEquals(
        400,
        sut.handle(
                request(
                    Map.of(),
                    "openingHour",
                    8,
                    "closingHour",
                    18,
                    "maxVehiclesPerHour",
                    3,
                    "openOnSunday",
                    "sim"))
            .statusCode());
  }
}
