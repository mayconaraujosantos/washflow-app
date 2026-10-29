package com.washflow.data.usecases.deletephoto;

import static org.mockito.Mockito.verify;

import com.washflow.data.protocols.db.DeleteServiceOrderPhotoRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbDeleteServiceOrderPhotoTest {

  @Mock private DeleteServiceOrderPhotoRepository deleteRepository;

  private DbDeleteServiceOrderPhoto sut;

  @BeforeEach
  void setUp() {
    sut = new DbDeleteServiceOrderPhoto(deleteRepository);
  }

  @Test
  void deletesPhoto() {
    UUID photoId = UUID.randomUUID();
    sut.delete(photoId);
    verify(deleteRepository).deleteById(photoId);
  }
}
