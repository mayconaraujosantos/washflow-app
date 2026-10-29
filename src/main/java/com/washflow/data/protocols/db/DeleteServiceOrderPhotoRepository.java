package com.washflow.data.protocols.db;

import java.util.UUID;

public interface DeleteServiceOrderPhotoRepository {

  void deleteById(UUID photoId);
}
