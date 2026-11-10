package com.washflow.domain.usecases.serviceorder;

import java.util.UUID;

public interface DeleteServiceOrderPhoto {

  void delete(UUID photoId);
}
