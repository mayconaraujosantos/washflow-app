package com.washflow.application.routes.requests;

import com.washflow.presentation.controllers.schedule.ScheduleServiceOrderController;
import java.util.List;

/**
 * Documents the JSON body {@link ScheduleServiceOrderController} expects. Not used at runtime - the
 * controller reads the raw {@code HttpRequest.body()} map directly, framework-agnostically - this
 * exists purely so {@code @OpenApiRequestBody} has a concrete class to derive a schema from.
 */
public record ScheduleServiceOrderRequest(
    String vehicleId, List<Integer> servicePriceIds, String scheduledAt) {}
