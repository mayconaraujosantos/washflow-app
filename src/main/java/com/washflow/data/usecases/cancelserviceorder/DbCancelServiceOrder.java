package com.washflow.data.usecases.cancelserviceorder;

import com.washflow.data.protocols.db.CancelServiceOrderRepository;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotCancellableError;
import com.washflow.domain.usecases.CancelServiceOrder;
import java.util.Set;

/**
 * Validates the cancellation reason and delegates to {@code CancelServiceOrderRepository}, whose
 * compare-and-set only cancels orders still scheduled or waiting in the yard.
 */
public class DbCancelServiceOrder implements CancelServiceOrder {

  private static final Set<String> VALID_REASONS =
      Set.of("CUSTOMER_GAVE_UP", "NO_SHOW", "REGISTERED_BY_MISTAKE", "OTHER");
  private static final int MAX_DESCRIPTION = 200;
  private static final String INVALID_REASON =
      "Escolha o motivo do cancelamento; em Outro, descreva em até 200 caracteres";

  private final CancelServiceOrderRepository repository;

  public DbCancelServiceOrder(CancelServiceOrderRepository repository) {
    this.repository = repository;
  }

  @Override
  public void cancel(Params params) throws ServiceOrderNotCancellableError {
    String reason = params.reason() == null ? "" : params.reason().trim();
    String description = params.description() == null ? null : params.description().trim();
    if (description != null && description.isEmpty()) {
      description = null;
    }
    if (!VALID_REASONS.contains(reason)
        || ("OTHER".equals(reason) && description == null)
        || (description != null && description.length() > MAX_DESCRIPTION)) {
      throw new IllegalArgumentException(INVALID_REASON);
    }
    if (!repository.cancel(params.serviceOrderId(), params.managerId(), reason, description)) {
      throw new ServiceOrderNotCancellableError();
    }
  }
}
