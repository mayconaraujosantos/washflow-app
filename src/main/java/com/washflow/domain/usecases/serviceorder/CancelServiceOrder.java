package com.washflow.domain.usecases.serviceorder;

import com.washflow.domain.errors.serviceorder.ServiceOrderNotCancellableError;
import java.util.UUID;

/**
 * Cancels a service order that never reached washing ({@code AGENDADO} or {@code
 * AGUARDANDO_PATIO}), recording the manager and the reason. Implemented by {@code
 * DbCancelServiceOrder} in the data layer.
 */
public interface CancelServiceOrder {

  void cancel(Params params) throws ServiceOrderNotCancellableError;

  record Params(UUID serviceOrderId, UUID managerId, String reason, String description) {}
}
