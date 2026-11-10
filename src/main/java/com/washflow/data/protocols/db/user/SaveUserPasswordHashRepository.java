package com.washflow.data.protocols.db.user;

import java.util.UUID;

public interface SaveUserPasswordHashRepository {

  void savePasswordHash(UUID userId, String passwordHash);
}
