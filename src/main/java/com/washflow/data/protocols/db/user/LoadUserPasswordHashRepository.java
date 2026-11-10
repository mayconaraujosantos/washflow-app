package com.washflow.data.protocols.db.user;

import java.util.Optional;
import java.util.UUID;

public interface LoadUserPasswordHashRepository {

  Optional<String> loadPasswordHash(UUID userId);
}
