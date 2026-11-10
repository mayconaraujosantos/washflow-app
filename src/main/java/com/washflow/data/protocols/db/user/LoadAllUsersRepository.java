package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.user.User;
import java.util.List;

/** Port for loading all users. */
public interface LoadAllUsersRepository {

  List<User> loadAll();
}
