package com.washflow.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code usuarios} from CLAUDE.md. {@code senha_hash} isn't modeled here - the QR-code login flow
 * (see {@code AuthenticateUser}) identifies a user by {@code phone} alone, no password.
 */
public record User(UUID id, String name, String phone, UserProfile profile, Instant createdAt) {}
