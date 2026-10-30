package com.washflow.data.protocols.db;

import com.washflow.domain.entities.User;
import java.util.Optional;
import java.util.UUID;

public interface LoadUserByIdRepository {

  Optional<User> loadById(UUID id);
}
