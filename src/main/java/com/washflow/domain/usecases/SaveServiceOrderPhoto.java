package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrderPhoto;
import com.washflow.domain.entities.ServiceOrderPhoto.PhotoType;
import com.washflow.domain.errors.InvalidPhotoTypeError;
import java.util.UUID;

public interface SaveServiceOrderPhoto {

  ServiceOrderPhoto save(Params params) throws InvalidPhotoTypeError;

  record Params(UUID serviceOrderId, PhotoType type, String imageData, String description, UUID createdBy) {}
}
