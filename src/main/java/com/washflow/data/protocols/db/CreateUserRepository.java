package com.washflow.data.protocols.db;

import com.washflow.domain.entities.User;

public interface CreateUserRepository {

  User create(User user);
}
