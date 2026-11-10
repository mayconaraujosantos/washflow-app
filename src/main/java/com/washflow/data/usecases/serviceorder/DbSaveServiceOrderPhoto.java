package com.washflow.data.usecases.serviceorder;

import com.washflow.data.protocols.db.serviceorder.SaveServiceOrderPhotoRepository;
import com.washflow.domain.entities.serviceorder.ServiceOrderPhoto;
import com.washflow.domain.errors.serviceorder.InvalidPhotoTypeError;
import com.washflow.domain.usecases.serviceorder.SaveServiceOrderPhoto;
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
