package com.washflow.domain.usecases.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.util.UUID;

/**
 * The manager reserves a car for one washer before the wash starts ({@code AGENDADO} or {@code
 * AGUARDANDO_PATIO}), or gives it back to the shared queue with {@code washerId} null. Only that
 * washer can then start it. Implemented by {@code DbAssignWasher} in the data layer.
 */
public interface AssignWasher {

  ServiceOrder assign(Params params);

  record Params(UUID serviceOrderId, UUID washerId) {}
}
