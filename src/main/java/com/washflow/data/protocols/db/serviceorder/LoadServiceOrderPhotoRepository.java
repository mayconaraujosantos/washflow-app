package com.washflow.data.protocols.db.serviceorder;

import com.washflow.domain.entities.ServiceOrderPhoto;
import java.util.List;
import java.util.UUID;

public interface LoadServiceOrderPhotoRepository {

  List<ServiceOrderPhoto> loadByServiceOrderId(UUID serviceOrderId);
}
