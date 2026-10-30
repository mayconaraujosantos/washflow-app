package com.washflow.presentation.controllers.updateuserprofile;

/**
 * Documents the JSON body {@link UpdateUserProfileController} expects. Not used at runtime - the
 * controller reads the raw {@code HttpRequest.body()} map directly, framework-agnostically - this
 * exists purely so {@code @OpenApiRequestBody} has a concrete class to derive a schema from.
 *
 * <p>{@code newProfile} is one of {@code CUSTOMER}, {@code WASHER}, {@code MANAGER} - {@link
 * com.washflow.domain.entities.UserProfile}'s Java-side names, the same convention {@code
 * ServiceOrder.status()} already serializes with.
 */
public record UpdateUserProfileRequest(String managerId, String newProfile) {}
