package com.washflow.application.factories.usecases.user;

import com.washflow.data.usecases.user.DbUpdateUserActive;
import com.washflow.domain.usecases.user.UpdateUserActive;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class UpdateUserActiveFactory {

  private UpdateUserActiveFactory() {}

  public static UpdateUserActive make(Jdbi jdbi) {
    UserJdbiRepository repository = new UserJdbiRepository(jdbi);
    return new DbUpdateUserActive(repository);
  }
}
