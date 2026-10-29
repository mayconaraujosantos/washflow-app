package com.washflow.data.protocols.db;

import com.washflow.domain.entities.User;

/** Port for creating a washer user with a password hash. */
public interface CreateWasherRepository {

  User createWasher(User user, String passwordHash);
}
