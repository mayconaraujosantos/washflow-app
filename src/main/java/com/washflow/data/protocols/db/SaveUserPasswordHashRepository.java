package com.washflow.data.protocols.db;

import java.util.UUID;

public interface SaveUserPasswordHashRepository {

  void savePasswordHash(UUID userId, String passwordHash);
}
