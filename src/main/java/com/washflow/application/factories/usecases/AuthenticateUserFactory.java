package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.authenticateuser.DbAuthenticateUser;
import com.washflow.domain.usecases.AuthenticateUser;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new UserJdbiRepository(...)}
 * call here - {@code DbAuthenticateUser} depends on the {@code data.protocols.db} interfaces, not
 * on this concrete class.
 */
public final class AuthenticateUserFactory {

  private AuthenticateUserFactory() {}

  public static AuthenticateUser make(Jdbi jdbi) {
    UserJdbiRepository userRepository = new UserJdbiRepository(jdbi);
    return new DbAuthenticateUser(userRepository, userRepository);
  }
}
