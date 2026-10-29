package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrderPhoto;

public interface SaveServiceOrderPhotoRepository {

  ServiceOrderPhoto save(ServiceOrderPhoto photo);
}
