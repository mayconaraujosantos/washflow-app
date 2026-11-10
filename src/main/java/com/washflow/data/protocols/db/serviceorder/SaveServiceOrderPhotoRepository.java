package com.washflow.data.protocols.db.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrderPhoto;

public interface SaveServiceOrderPhotoRepository {

  ServiceOrderPhoto save(ServiceOrderPhoto photo);
}
