package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.user.User;
import java.util.UUID;

public interface UpdateUserEmailRepository {

  User updateEmail(UUID userId, String email);
}
