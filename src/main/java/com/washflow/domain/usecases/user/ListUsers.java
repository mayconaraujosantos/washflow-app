package com.washflow.domain.usecases.user;

import com.washflow.domain.entities.User;
import java.util.List;

/** Returns all users. Implemented by {@code DbListUsers} in the data layer. */
public interface ListUsers {

  List<User> list();
}
