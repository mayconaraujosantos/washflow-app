package com.washflow.presentation.controllers.createvehicle;

/**
 * Documents the JSON body {@link CreateVehicleController} expects. Not used at runtime - the
 * controller reads the raw {@code HttpRequest.body()} map directly, framework-agnostically - this
 * exists purely so {@code @OpenApiRequestBody} has a concrete class to derive a schema from.
 */
public record CreateVehicleRequest(String customerId, String plate, String model, String color) {}
