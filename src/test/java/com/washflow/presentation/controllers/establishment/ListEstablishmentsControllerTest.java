package com.washflow.presentation.controllers.establishment;

import static com.washflow.presentation.helpers.HttpRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.establishment.ManagePlatform;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ListEstablishmentsControllerTest {

  @Test
  void listsEveryLavaJatoForTheAdministrator() {
    ManagePlatform platform = mock(ManagePlatform.class);
    when(platform.list()).thenReturn(List.of());

    HttpResponse response = new ListEstablishmentsController(platform).handle(request(Map.of()));

    assertEquals(200, response.statusCode());
    assertEquals(List.of(), response.body());
  }
}
