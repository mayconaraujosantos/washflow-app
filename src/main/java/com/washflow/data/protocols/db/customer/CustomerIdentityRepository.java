package com.washflow.data.protocols.db.customer;

import com.washflow.domain.entities.user.User;
import java.util.Optional;
import java.util.UUID;

public interface CustomerIdentityRepository {

  Optional<User> loadByGoogleSubject(String subject);

  void linkGoogleSubject(UUID userId, String subject);

  /** Account of this lava-jato with the (normalized) e-mail, whatever its profile. */
  Optional<User> loadByEmail(String email);
}
