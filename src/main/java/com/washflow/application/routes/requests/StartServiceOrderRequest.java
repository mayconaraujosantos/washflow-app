package com.washflow.application.routes.requests;

import com.washflow.presentation.controllers.serviceorder.StartServiceOrderController;

/**
 * Documents the JSON body {@link StartServiceOrderController} expects. Not used at runtime - the
 * controller reads the raw {@code HttpRequest.body()} map directly, framework-agnostically - this
 * exists purely so {@code @OpenApiRequestBody} has a concrete class to derive a schema from.
 */
public record StartServiceOrderRequest(String washerId) {}
