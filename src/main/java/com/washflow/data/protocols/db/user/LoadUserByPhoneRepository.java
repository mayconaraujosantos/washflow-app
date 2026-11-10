package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.user.User;
import java.util.Optional;

public interface LoadUserByPhoneRepository {

  Optional<User> loadByPhone(String phone);
}
