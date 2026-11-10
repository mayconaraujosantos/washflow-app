package com.washflow.data.protocols.db.access;

import com.washflow.domain.entities.User;
import java.util.Optional;
import java.util.UUID;

public interface CustomerIdentityRepository {

  Optional<User> loadByGoogleSubject(String subject);

  void linkGoogleSubject(UUID userId, String subject);
}
