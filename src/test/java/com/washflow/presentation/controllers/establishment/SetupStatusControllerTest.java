package com.washflow.presentation.controllers.establishment;

import static com.washflow.presentation.helpers.HttpRequests.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.establishment.SetupEstablishment;
import com.washflow.presentation.protocols.HttpResponse;
import org.junit.jupiter.api.Test;

class SetupStatusControllerTest {

  @Test
  void tellsWhetherThePlatformIsConfigured() {
    SetupEstablishment setup = mock(SetupEstablishment.class);
    SetupEstablishment.Status status = new SetupEstablishment.Status(false, true, null);
    when(setup.status()).thenReturn(status);

    HttpResponse response = new SetupStatusController(setup).handle(query());

    assertEquals(200, response.statusCode());
    assertEquals(status, response.body());
  }
}
