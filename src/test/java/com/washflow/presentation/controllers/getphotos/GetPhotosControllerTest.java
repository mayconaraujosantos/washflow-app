package com.washflow.presentation.controllers.getphotos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrderPhoto;
import com.washflow.domain.entities.ServiceOrderPhoto.PhotoType;
import com.washflow.domain.usecases.ListServiceOrderPhotos;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPhotosControllerTest {

  @Mock private ListServiceOrderPhotos listServiceOrderPhotos;

  private GetPhotosController sut;
  private UUID serviceOrderId;

  @BeforeEach
  void setUp() {
    sut = new GetPhotosController(listServiceOrderPhotos);
    serviceOrderId = UUID.randomUUID();
  }

  @Test
  void returnsPhotosWhenFound() {
    List<ServiceOrderPhoto> photos =
        List.of(
            new ServiceOrderPhoto(
                UUID.randomUUID(),
                serviceOrderId,
                PhotoType.ENTRY,
                "base64",
                null,
                Instant.now(),
                null));
    when(listServiceOrderPhotos.list(any(UUID.class))).thenReturn(photos);
    HttpRequest request =
        new HttpRequest(Collections.emptyMap(), Map.of("id", serviceOrderId.toString()), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(listServiceOrderPhotos).list(serviceOrderId);
  }

  @Test
  void returnsEmptyListWhenNoPhotos() {
    when(listServiceOrderPhotos.list(any(UUID.class))).thenReturn(Collections.emptyList());
    HttpRequest request =
        new HttpRequest(Collections.emptyMap(), Map.of("id", serviceOrderId.toString()), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenInvalidId() {
    HttpRequest request =
        new HttpRequest(Collections.emptyMap(), Map.of("id", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
