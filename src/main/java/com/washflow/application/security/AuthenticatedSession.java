package com.washflow.application.security;

import com.washflow.domain.entities.user.User;

public record AuthenticatedSession(User user, String token) {}
