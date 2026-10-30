package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.listphotos.DbListServiceOrderPhotos;
import com.washflow.domain.usecases.ListServiceOrderPhotos;
import com.washflow.infra.db.jdbi.PhotoJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ListServiceOrderPhotosFactory {

  private ListServiceOrderPhotosFactory() {}

  public static ListServiceOrderPhotos make(Jdbi jdbi) {
    PhotoJdbiRepository repository = new PhotoJdbiRepository(jdbi);
    return new DbListServiceOrderPhotos(repository);
  }
}
