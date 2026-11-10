package com.washflow.application.routes.requests;

import com.washflow.presentation.controllers.vehicle.CreateVehicleController;

/**
 * Documents the JSON body {@link CreateVehicleController} expects. Not used at runtime - the
 * controller reads the raw {@code HttpRequest.body()} map directly, framework-agnostically - this
 * exists purely so {@code @OpenApiRequestBody} has a concrete class to derive a schema from.
 */
public record CreateVehicleRequest(String customerId, String plate, String model, String color) {}
