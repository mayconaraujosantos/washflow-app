package com.washflow.domain.usecases.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrderPhoto;
import com.washflow.domain.entities.serviceorder.ServiceOrderPhoto.PhotoType;
import com.washflow.domain.errors.serviceorder.InvalidPhotoTypeError;
import java.util.UUID;

public interface SaveServiceOrderPhoto {

  ServiceOrderPhoto save(Params params) throws InvalidPhotoTypeError;

  record Params(
      UUID serviceOrderId, PhotoType type, String imageData, String description, UUID createdBy) {}
}
