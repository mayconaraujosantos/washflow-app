package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.savephoto.DbSaveServiceOrderPhoto;
import com.washflow.domain.usecases.SaveServiceOrderPhoto;
import com.washflow.infra.db.jdbi.PhotoJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class SaveServiceOrderPhotoFactory {

  private SaveServiceOrderPhotoFactory() {}

  public static SaveServiceOrderPhoto make(Jdbi jdbi) {
    PhotoJdbiRepository repository = new PhotoJdbiRepository(jdbi);
    return new DbSaveServiceOrderPhoto(repository);
  }
}
