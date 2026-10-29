package com.washflow.data.protocols.db;

import com.washflow.domain.entities.User;
import java.util.UUID;

/** Port for updating a user's active status. */
public interface UpdateUserActiveRepository {

  User updateActive(UUID userId, boolean active);
}
