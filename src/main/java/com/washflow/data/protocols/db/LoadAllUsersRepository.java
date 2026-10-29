package com.washflow.data.protocols.db;

import com.washflow.domain.entities.User;
import java.util.List;

/** Port for loading all users. */
public interface LoadAllUsersRepository {

  List<User> loadAll();
}
