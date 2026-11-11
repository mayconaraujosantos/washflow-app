package com.washflow.presentation.controllers.establishment;

import static com.washflow.presentation.helpers.HttpRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.establishment.Establishment;
import com.washflow.domain.usecases.establishment.ManageEstablishment;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GetEstablishmentControllerTest {

  @Test
  void returnsTheLavaJatoOfTheRequest() {
    ManageEstablishment manage = mock(ManageEstablishment.class);
    Establishment establishment = mock(Establishment.class);
    when(manage.get()).thenReturn(establishment);

    HttpResponse response = new GetEstablishmentController(manage).handle(request(Map.of()));

    assertEquals(200, response.statusCode());
    assertEquals(establishment, response.body());
  }
}
