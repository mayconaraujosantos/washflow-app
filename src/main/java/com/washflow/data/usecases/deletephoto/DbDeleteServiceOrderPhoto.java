package com.washflow.data.usecases.deletephoto;

import com.washflow.data.protocols.db.DeleteServiceOrderPhotoRepository;
import com.washflow.domain.usecases.DeleteServiceOrderPhoto;
import java.util.UUID;

public class DbDeleteServiceOrderPhoto implements DeleteServiceOrderPhoto {

  private final DeleteServiceOrderPhotoRepository deleteRepository;

  public DbDeleteServiceOrderPhoto(DeleteServiceOrderPhotoRepository deleteRepository) {
    this.deleteRepository = deleteRepository;
  }

  @Override
  public void delete(UUID photoId) {
    deleteRepository.deleteById(photoId);
  }
}
