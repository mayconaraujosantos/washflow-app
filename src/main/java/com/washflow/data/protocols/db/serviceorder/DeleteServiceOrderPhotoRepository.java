package com.washflow.data.protocols.db.serviceorder;

import java.util.UUID;

public interface DeleteServiceOrderPhotoRepository {

  void deleteById(UUID photoId);
}
