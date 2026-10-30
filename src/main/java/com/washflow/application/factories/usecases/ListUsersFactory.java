package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.listusers.DbListUsers;
import com.washflow.domain.usecases.ListUsers;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ListUsersFactory {

  private ListUsersFactory() {}

  public static ListUsers make(Jdbi jdbi) {
    UserJdbiRepository repository = new UserJdbiRepository(jdbi);
    return new DbListUsers(repository);
  }
}
