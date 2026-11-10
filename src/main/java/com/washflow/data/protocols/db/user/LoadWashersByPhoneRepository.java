package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.user.User;
import java.util.List;

public interface LoadWashersByPhoneRepository {

  /**
   * Washer accounts with this phone in active lava-jatos (only the current one inside a tenant).
   * One person may wash at two lava-jatos with the same phone: each account has its own badge.
   */
  List<User> loadWashersByPhone(String phone);
}
