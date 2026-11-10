package com.washflow.domain.usecases.user;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.errors.user.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.user.UserNotFoundError;
import java.util.UUID;

/** A manager sets (or clears) the login e-mail of an account. */
public interface UpdateUserEmail {

  User update(UUID userId, String email) throws UserNotFoundError, EmailAlreadyRegisteredError;
}
