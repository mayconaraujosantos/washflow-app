package com.washflow.domain.usecases.user;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.errors.user.AccountLockedError;
import java.util.UUID;

/**
 * Washer access without a password: the manager issues a badge (6-digit PIN + QR token, shown
 * once); the washer signs in with phone + PIN or by scanning the badge. Re-issuing invalidates the
 * previous badge.
 */
public interface WasherBadge {

  Issued issue(UUID washerId);

  User signInWithPin(String phone, String pin) throws AccountLockedError;

  User signInWithBadge(String badgeToken);

  record Issued(String pin, String badgeToken) {}
}
