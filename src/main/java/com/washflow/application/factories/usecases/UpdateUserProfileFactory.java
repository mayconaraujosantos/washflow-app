package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.updateuserprofile.DbUpdateUserProfile;
import com.washflow.domain.usecases.UpdateUserProfile;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
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
