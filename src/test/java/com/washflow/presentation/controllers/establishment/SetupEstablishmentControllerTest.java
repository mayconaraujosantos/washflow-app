package com.washflow.presentation.controllers.establishment;

import static com.washflow.presentation.helpers.HttpRequests.body;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.errors.establishment.EstablishmentAlreadyConfiguredError;
import com.washflow.domain.errors.establishment.InvalidSetupCodeError;
import com.washflow.domain.usecases.establishment.SetupEstablishment;
import com.washflow.presentation.protocols.HttpResponse;
import org.junit.jupiter.api.Test;

class SetupEstablishmentControllerTest {

  @Test
  void configuresThePlatformOwner() throws Exception {
    SetupEstablishment setup = mock(SetupEstablishment.class);

    HttpResponse response =
        new SetupEstablishmentController(setup)
            .handle(
                body(
                    "setupCode",
                    "c",
                    "ownerName",
                    "Ana",
                    "ownerEmail",
                    "a@x.test",
                    "ownerPassword",
                    "secret-123"));

    assertEquals(200, response.statusCode());
    verify(setup).setup(new SetupEstablishment.Params("c", "Ana", "a@x.test", "secret-123"));
  }

  @Test
  void mapsWrongCodeAlreadyConfiguredAndWrongTypes() throws Exception {
    SetupEstablishment setup = mock(SetupEstablishment.class);
    when(setup.setup(any()))
        .thenThrow(new InvalidSetupCodeError())
        .thenThrow(new EstablishmentAlreadyConfiguredError());
    SetupEstablishmentController sut = new SetupEstablishmentController(setup);

    assertEquals(403, sut.handle(body("setupCode", "x")).statusCode());
    assertEquals(409, sut.handle(body("setupCode", "x")).statusCode());
    assertEquals(400, sut.handle(body("setupCode", 1)).statusCode());
  }
}
