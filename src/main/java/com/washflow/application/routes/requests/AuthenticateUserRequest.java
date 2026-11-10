package com.washflow.application.routes.requests;

import com.washflow.presentation.controllers.user.AuthenticateUserController;

/**
 * Documents the JSON body {@link AuthenticateUserController} expects. Not used at runtime - the
 * controller reads the raw {@code HttpRequest.body()} map directly - this exists so
 * {@code @OpenApiRequestBody} has a concrete class to derive a schema from.
 */
public record AuthenticateUserRequest(String phone, String name, String password) {}
