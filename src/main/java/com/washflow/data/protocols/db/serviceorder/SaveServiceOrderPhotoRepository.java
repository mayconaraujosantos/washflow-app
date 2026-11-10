package com.washflow.data.protocols.db.serviceorder;

import com.washflow.domain.entities.ServiceOrderPhoto;

public interface SaveServiceOrderPhotoRepository {

  ServiceOrderPhoto save(ServiceOrderPhoto photo);
}
