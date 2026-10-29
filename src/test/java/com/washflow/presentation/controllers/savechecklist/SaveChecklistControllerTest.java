package com.washflow.presentation.controllers.savechecklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrderChecklist;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistItem;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistType;
import com.washflow.domain.errors.InvalidChecklistTypeError;
import com.washflow.domain.usecases.SaveServiceOrderChecklist;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaveChecklistControllerTest {

  @Mock private SaveServiceOrderChecklist saveServiceOrderChecklist;

  private SaveChecklistController sut;
  private UUID serviceOrderId;
  private UUID createdBy;

  @BeforeEach
  void setUp() {
    sut = new SaveChecklistController(saveServiceOrderChecklist);
    serviceOrderId = UUID.randomUUID();
    createdBy = UUID.randomUUID();
  }

  @Test
  void savesChecklistAndReturnsOk() throws Exception {
    List<Map<String, Object>> itemsMap =
        Arrays.asList(
            new HashMap<String, Object>() {
              {
                put("label", "Faróis");
                put("checked", true);
              }
            },
            new HashMap<String, Object>() {
              {
                put("label", "Retrovisores");
                put("checked", false);
                put("note", "Verificar");
              }
            });

    Map<String, Object> body = new HashMap<>();
    body.put("type", "ENTRY");
    body.put("items", itemsMap);
    body.put("signatureImage", "base64image");
    body.put("createdBy", createdBy);
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()));

    ServiceOrderChecklist saved =
        new ServiceOrderChecklist(
            UUID.randomUUID(),
            serviceOrderId,
            ChecklistType.ENTRY,
            List.of(
                new ChecklistItem("Faróis", true, null),
                new ChecklistItem("Retrovisores", false, "Verificar")),
            "base64image",
            Instant.now(),
            createdBy);
    when(saveServiceOrderChecklist.save(any(SaveServiceOrderChecklist.Params.class)))
        .thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(201, response.statusCode());
    assertNotNull(response.body());
    verify(saveServiceOrderChecklist).save(any(SaveServiceOrderChecklist.Params.class));
  }

  @Test
  void returnsBadRequestWhenTypeIsMissing() {
    Map<String, Object> item = new HashMap<>();
    item.put("label", "Faróis");
    item.put("checked", true);

    Map<String, Object> body = new HashMap<>();
    body.put("items", List.of(item));
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenItemsAreMissing() {
    Map<String, Object> body = new HashMap<>();
    body.put("type", "ENTRY");
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenServiceOrderIdIsInvalid() {
    Map<String, Object> item = new HashMap<>();
    item.put("label", "Faróis");
    item.put("checked", true);

    Map<String, Object> body = new HashMap<>();
    body.put("type", "ENTRY");
    body.put("items", List.of(item));
    HttpRequest request = new HttpRequest(body, Map.of("id", "not-a-uuid"));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenUseCaseThrowsInvalidChecklistType() throws Exception {
    Map<String, Object> item = new HashMap<>();
    item.put("label", "Faróis");
    item.put("checked", true);

    Map<String, Object> body = new HashMap<>();
    body.put("type", "ENTRY");
    body.put("items", List.of(item));
    body.put("signatureImage", null);
    body.put("createdBy", createdBy);
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()));

    when(saveServiceOrderChecklist.save(any(SaveServiceOrderChecklist.Params.class)))
        .thenThrow(new InvalidChecklistTypeError("empty items"));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
