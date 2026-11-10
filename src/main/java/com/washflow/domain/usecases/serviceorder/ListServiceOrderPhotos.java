package com.washflow.domain.usecases.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrderPhoto;
import java.util.List;
import java.util.UUID;

public interface ListServiceOrderPhotos {

  List<ServiceOrderPhoto> list(UUID serviceOrderId);
}
