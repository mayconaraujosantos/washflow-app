package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.user.User;
import java.util.UUID;

/** Port for updating a user's active status. */
public interface UpdateUserActiveRepository {

  User updateActive(UUID userId, boolean active);
}
