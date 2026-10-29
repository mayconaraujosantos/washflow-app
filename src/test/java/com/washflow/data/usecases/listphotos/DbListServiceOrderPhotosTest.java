package com.washflow.data.usecases.listphotos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadServiceOrderPhotoRepository;
import com.washflow.domain.entities.ServiceOrderPhoto;
import com.washflow.domain.entities.ServiceOrderPhoto.PhotoType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbListServiceOrderPhotosTest {

  @Mock private LoadServiceOrderPhotoRepository loadRepository;

  private DbListServiceOrderPhotos sut;
  private UUID serviceOrderId;

  @BeforeEach
  void setUp() {
    sut = new DbListServiceOrderPhotos(loadRepository);
    serviceOrderId = UUID.randomUUID();
  }

  @Test
  void listsPhotosByServiceOrderId() {
    List<ServiceOrderPhoto> expected =
        List.of(
            new ServiceOrderPhoto(
                UUID.randomUUID(),
                serviceOrderId,
                PhotoType.ENTRY,
                "base64data",
                null,
                Instant.now(),
                null));
    when(loadRepository.loadByServiceOrderId(eq(serviceOrderId))).thenReturn(expected);

    List<ServiceOrderPhoto> result = sut.list(serviceOrderId);

    assertEquals(1, result.size());
    assertEquals(PhotoType.ENTRY, result.get(0).type());
    verify(loadRepository).loadByServiceOrderId(serviceOrderId);
  }

  @Test
  void returnsEmptyListWhenNoPhotos() {
    when(loadRepository.loadByServiceOrderId(eq(serviceOrderId)))
        .thenReturn(Collections.emptyList());

    List<ServiceOrderPhoto> result = sut.list(serviceOrderId);

    assertEquals(0, result.size());
  }
}
