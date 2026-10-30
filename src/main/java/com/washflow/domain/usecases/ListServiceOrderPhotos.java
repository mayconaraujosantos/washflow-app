package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrderPhoto;
import java.util.List;
import java.util.UUID;

public interface ListServiceOrderPhotos {

  List<ServiceOrderPhoto> list(UUID serviceOrderId);
}
