package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.user.User;

public interface CreateUserRepository {

  User create(User user, String passwordHash);
}
