package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.user.User;
import java.util.Optional;

public interface LoadUserByEmailRepository {

  /** {@code email} must already be normalized to lower case. */
  Optional<User> loadByEmail(String email);
}
