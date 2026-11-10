package com.washflow.application.factories.usecases.user;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.usecases.user.DbCreateWasher;
import com.washflow.domain.usecases.user.CreateWasher;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.security.BCryptPasswordHasher;
import org.jdbi.v3.core.Jdbi;

public final class CreateWasherFactory {

  private CreateWasherFactory() {}

  public static CreateWasher make(Jdbi jdbi) {
    UserJdbiRepository repository = new UserJdbiRepository(jdbi);
    PasswordHasher passwordHasher = new BCryptPasswordHasher();
    return new DbCreateWasher(repository, repository, passwordHasher);
  }
}
