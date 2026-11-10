package com.washflow.application.factories.usecases.serviceorder;

import com.washflow.data.usecases.serviceorder.DbSaveServiceOrderPhoto;
import com.washflow.domain.usecases.serviceorder.SaveServiceOrderPhoto;
import com.washflow.infra.db.jdbi.serviceorder.PhotoJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class SaveServiceOrderPhotoFactory {

  private SaveServiceOrderPhotoFactory() {}

  public static SaveServiceOrderPhoto make(Jdbi jdbi) {
    PhotoJdbiRepository repository = new PhotoJdbiRepository(jdbi);
    return new DbSaveServiceOrderPhoto(repository);
  }
}
