package com.washflow.domain.usecases.user;

import com.washflow.domain.entities.user.User;

/** Creates a new washer user. Implemented by {@code DbCreateWasher} in the data layer. */
public interface CreateWasher {

  User create(Params params);

  record Params(String name, String phone, String password) {}
}
