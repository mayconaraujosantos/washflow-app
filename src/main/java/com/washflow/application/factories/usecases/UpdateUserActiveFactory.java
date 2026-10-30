package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.updateuseractive.DbUpdateUserActive;
import com.washflow.domain.usecases.UpdateUserActive;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class UpdateUserActiveFactory {

  private UpdateUserActiveFactory() {}

  public static UpdateUserActive make(Jdbi jdbi) {
    UserJdbiRepository repository = new UserJdbiRepository(jdbi);
    return new DbUpdateUserActive(repository);
  }
}
