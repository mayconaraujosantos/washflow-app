package com.washflow.infra.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.washflow.data.protocols.PasswordHasher;

public final class BCryptPasswordHasher implements PasswordHasher {

  @Override
  public String hash(String password) {
    return BCrypt.withDefaults().hashToString(12, password.toCharArray());
  }

  @Override
  public boolean matches(String password, String passwordHash) {
    return BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified;
  }
}
