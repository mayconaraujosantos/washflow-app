package com.washflow.presentation.controllers.deletephoto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.washflow.domain.usecases.DeleteServiceOrderPhoto;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeletePhotoControllerTest {

  @Mock private DeleteServiceOrderPhoto deleteServiceOrderPhoto;

  private DeletePhotoController sut;
  private UUID photoId;

  @BeforeEach
  void setUp() {
    sut = new DeletePhotoController(deleteServiceOrderPhoto);
    photoId = UUID.randomUUID();
  }

  @Test
  void deletesPhotoAndReturnsNoContent() {
    HttpRequest request =
        new HttpRequest(Collections.emptyMap(), Map.of("photoId", photoId.toString()), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(204, response.statusCode());
    verify(deleteServiceOrderPhoto).delete(photoId);
  }

  @Test
  void returnsBadRequestWhenInvalidId() {
    HttpRequest request =
        new HttpRequest(Collections.emptyMap(), Map.of("photoId", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
