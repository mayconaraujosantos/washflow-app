package com.washflow.domain.usecases.user;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.errors.user.PhoneAlreadyRegisteredError;

/** Creates a new washer user. Implemented by {@code DbCreateWasher} in the data layer. */
public interface CreateWasher {

  User create(Params params) throws PhoneAlreadyRegisteredError;

  record Params(String name, String phone, String password) {}
}
