package com.washflow.presentation.controllers.savephoto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrderPhoto;
import com.washflow.domain.entities.ServiceOrderPhoto.PhotoType;
import com.washflow.domain.errors.InvalidPhotoTypeError;
import com.washflow.domain.usecases.SaveServiceOrderPhoto;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavePhotoControllerTest {

  @Mock private SaveServiceOrderPhoto saveServiceOrderPhoto;

  private SavePhotoController sut;
  private UUID serviceOrderId;
  private UUID createdBy;

  @BeforeEach
  void setUp() {
    sut = new SavePhotoController(saveServiceOrderPhoto);
    serviceOrderId = UUID.randomUUID();
    createdBy = UUID.randomUUID();
  }

  @Test
  void savesPhotoAndReturnsCreated() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("type", "ENTRY");
    body.put("imageData", "base64data");
    body.put("description", "Raspado");
    body.put("createdBy", createdBy);
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()));

    ServiceOrderPhoto saved =
        new ServiceOrderPhoto(
            UUID.randomUUID(),
            serviceOrderId,
            PhotoType.ENTRY,
            "base64data",
            "Raspado",
            Instant.now(),
            createdBy);
    when(saveServiceOrderPhoto.save(any(SaveServiceOrderPhoto.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(201, response.statusCode());
    verify(saveServiceOrderPhoto).save(any(SaveServiceOrderPhoto.Params.class));
  }

  @Test
  void returnsBadRequestWhenTypeIsMissing() {
    Map<String, Object> body = new HashMap<>();
    body.put("imageData", "base64data");
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenImageDataIsMissing() {
    Map<String, Object> body = new HashMap<>();
    body.put("type", "ENTRY");
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenUseCaseThrows() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("type", "ENTRY");
    body.put("imageData", "base64data");
    body.put("createdBy", createdBy);
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()));

    when(saveServiceOrderPhoto.save(any(SaveServiceOrderPhoto.Params.class)))
        .thenThrow(new InvalidPhotoTypeError("test"));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
