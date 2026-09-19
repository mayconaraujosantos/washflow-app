package com.washflow.presentation.controllers.scheduleserviceorder;

/**
 * Documents the JSON body {@link ScheduleServiceOrderController} expects. Not used at runtime - the
 * controller reads the raw {@code HttpRequest.body()} map directly, framework-agnostically - this
 * exists purely so {@code @OpenApiRequestBody} has a concrete class to derive a schema from.
 */
public record ScheduleServiceOrderRequest(
    String vehicleId, int servicePriceId, String scheduledAt) {}
