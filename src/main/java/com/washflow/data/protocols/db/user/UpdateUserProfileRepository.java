package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import java.util.UUID;

public interface UpdateUserProfileRepository {

  User updateProfile(UUID id, UserProfile profile);
}
