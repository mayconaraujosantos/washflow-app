package com.washflow.data.usecases.savephoto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.SaveServiceOrderPhotoRepository;
import com.washflow.domain.entities.ServiceOrderPhoto;
import com.washflow.domain.entities.ServiceOrderPhoto.PhotoType;
import com.washflow.domain.errors.InvalidPhotoTypeError;
import com.washflow.domain.usecases.SaveServiceOrderPhoto;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbSaveServiceOrderPhotoTest {

  @Mock private SaveServiceOrderPhotoRepository saveRepository;

  private DbSaveServiceOrderPhoto sut;
  private UUID serviceOrderId;
  private UUID createdBy;

  @BeforeEach
  void setUp() {
    sut = new DbSaveServiceOrderPhoto(saveRepository);
    serviceOrderId = UUID.randomUUID();
    createdBy = UUID.randomUUID();
  }

  @Test
  void savesPhotoWhenParamsAreValid() throws Exception {
    SaveServiceOrderPhoto.Params params =
        new SaveServiceOrderPhoto.Params(
            serviceOrderId, PhotoType.ENTRY, "base64data", "Raspado no para-choque", createdBy);

    ServiceOrderPhoto saved =
        new ServiceOrderPhoto(
            UUID.randomUUID(),
            serviceOrderId,
            PhotoType.ENTRY,
            "base64data",
            "Raspado no para-choque",
            Instant.now(),
            createdBy);
    when(saveRepository.save(any(ServiceOrderPhoto.class))).thenReturn(saved);

    ServiceOrderPhoto result = sut.save(params);

    assertEquals(PhotoType.ENTRY, result.type());
    verify(saveRepository).save(any(ServiceOrderPhoto.class));
  }

  @Test
  void throwsInvalidPhotoTypeWhenTypeIsNull() {
    SaveServiceOrderPhoto.Params params =
        new SaveServiceOrderPhoto.Params(serviceOrderId, null, "base64data", null, createdBy);

    assertThrows(InvalidPhotoTypeError.class, () -> sut.save(params));
    verifyNoInteractions(saveRepository);
  }

  @Test
  void throwsInvalidPhotoTypeWhenImageDataIsBlank() {
    SaveServiceOrderPhoto.Params params =
        new SaveServiceOrderPhoto.Params(serviceOrderId, PhotoType.ENTRY, "", null, createdBy);

    assertThrows(InvalidPhotoTypeError.class, () -> sut.save(params));
    verifyNoInteractions(saveRepository);
  }
}
