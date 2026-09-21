package com.washflow.presentation.controllers.authenticateuser;

/**
 * Documents the JSON body {@link AuthenticateUserController} expects. Not used at runtime - the
 * controller reads the raw {@code HttpRequest.body()} map directly, framework-agnostically - this
 * exists purely so {@code @OpenApiRequestBody} has a concrete class to derive a schema from.
 */
public record AuthenticateUserRequest(String phone, String name) {}
