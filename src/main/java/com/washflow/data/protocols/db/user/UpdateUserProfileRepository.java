package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import java.util.UUID;

public interface UpdateUserProfileRepository {

  User updateProfile(UUID id, UserProfile profile);
}
