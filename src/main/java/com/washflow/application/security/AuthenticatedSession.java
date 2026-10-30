package com.washflow.application.security;

import com.washflow.domain.entities.User;

public record AuthenticatedSession(User user, String token) {}
