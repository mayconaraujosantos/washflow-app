package com.washflow.data.usecases.savephoto;

import com.washflow.data.protocols.db.SaveServiceOrderPhotoRepository;
import com.washflow.domain.entities.ServiceOrderPhoto;
import com.washflow.domain.errors.InvalidPhotoTypeError;
import com.washflow.domain.usecases.SaveServiceOrderPhoto;
import java.time.Instant;
import java.util.UUID;

public class DbSaveServiceOrderPhoto implements SaveServiceOrderPhoto {

  private final SaveServiceOrderPhotoRepository saveRepository;

  public DbSaveServiceOrderPhoto(SaveServiceOrderPhotoRepository saveRepository) {
    this.saveRepository = saveRepository;
  }

  @Override
  public ServiceOrderPhoto save(Params params) throws InvalidPhotoTypeError {
    if (params.type() == null) {
      throw new InvalidPhotoTypeError(null);
    }
    if (params.imageData() == null || params.imageData().isBlank()) {
      throw new InvalidPhotoTypeError("imageData is required");
    }

    ServiceOrderPhoto photo =
        new ServiceOrderPhoto(
            UUID.randomUUID(),
            params.serviceOrderId(),
            params.type(),
            params.imageData(),
            params.description(),
            Instant.now(),
            params.createdBy());

    return saveRepository.save(photo);
  }
}
