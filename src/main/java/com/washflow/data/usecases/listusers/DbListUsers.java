package com.washflow.data.usecases.listusers;

import com.washflow.data.protocols.db.LoadAllUsersRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.usecases.ListUsers;
import java.util.List;

/**
 * Delegates to {@code LoadAllUsersRepository}. Depends only on the port interface, never on JDBI or
 * Postgres.
 */
public class DbListUsers implements ListUsers {

  private final LoadAllUsersRepository loadRepository;

  public DbListUsers(LoadAllUsersRepository loadRepository) {
    this.loadRepository = loadRepository;
  }

  @Override
  public List<User> list() {
    return loadRepository.loadAll();
  }
}
