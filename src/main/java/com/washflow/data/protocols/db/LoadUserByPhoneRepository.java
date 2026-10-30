package com.washflow.data.protocols.db;

import com.washflow.domain.entities.User;
import java.util.Optional;

public interface LoadUserByPhoneRepository {

  Optional<User> loadByPhone(String phone);
}
