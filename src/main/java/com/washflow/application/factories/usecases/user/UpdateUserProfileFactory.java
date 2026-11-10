package com.washflow.application.factories.usecases.user;

import com.washflow.data.usecases.user.DbUpdateUserProfile;
import com.washflow.domain.usecases.user.UpdateUserProfile;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new UserJdbiRepository(...)}
 * call here - {@code DbUpdateUserProfile} depends on the {@code data.protocols.db} interfaces, not
 * on this concrete class.
 */
public final class UpdateUserProfileFactory {

  private UpdateUserProfileFactory() {}

  public static UpdateUserProfile make(Jdbi jdbi) {
    UserJdbiRepository userRepository = new UserJdbiRepository(jdbi);
    return new DbUpdateUserProfile(userRepository, userRepository);
  }
}
