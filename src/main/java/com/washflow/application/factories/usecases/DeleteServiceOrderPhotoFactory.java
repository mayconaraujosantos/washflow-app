package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.deletephoto.DbDeleteServiceOrderPhoto;
import com.washflow.domain.usecases.DeleteServiceOrderPhoto;
import com.washflow.infra.db.jdbi.PhotoJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class DeleteServiceOrderPhotoFactory {

  private DeleteServiceOrderPhotoFactory() {}

  public static DeleteServiceOrderPhoto make(Jdbi jdbi) {
    PhotoJdbiRepository repository = new PhotoJdbiRepository(jdbi);
    return new DbDeleteServiceOrderPhoto(repository);
  }
}
