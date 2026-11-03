package com.washflow.data.protocols.db;

import com.washflow.domain.entities.User;
import java.util.UUID;

public interface UpdateUserEmailRepository {

  User updateEmail(UUID userId, String email);
}
