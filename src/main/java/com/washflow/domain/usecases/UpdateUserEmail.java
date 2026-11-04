package com.washflow.domain.usecases;

import com.washflow.domain.entities.User;
import com.washflow.domain.errors.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.UserNotFoundError;
import java.util.UUID;

/** A manager sets (or clears) the login e-mail of an account. */
public interface UpdateUserEmail {

  User update(UUID userId, String email) throws UserNotFoundError, EmailAlreadyRegisteredError;
}
