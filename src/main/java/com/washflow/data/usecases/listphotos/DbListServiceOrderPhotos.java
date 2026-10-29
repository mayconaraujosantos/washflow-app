package com.washflow.data.usecases.listphotos;

import com.washflow.data.protocols.db.LoadServiceOrderPhotoRepository;
import com.washflow.domain.entities.ServiceOrderPhoto;
import com.washflow.domain.usecases.ListServiceOrderPhotos;
import java.util.List;
import java.util.UUID;

public class DbListServiceOrderPhotos implements ListServiceOrderPhotos {

  private final LoadServiceOrderPhotoRepository loadRepository;

  public DbListServiceOrderPhotos(LoadServiceOrderPhotoRepository loadRepository) {
    this.loadRepository = loadRepository;
  }

  @Override
  public List<ServiceOrderPhoto> list(UUID serviceOrderId) {
    return loadRepository.loadByServiceOrderId(serviceOrderId);
  }
}
