package com.washflow.data.usecases.serviceorder;

import com.washflow.data.protocols.db.RefundServiceOrderRepository;
import com.washflow.domain.entities.ServiceOrderRefund;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotRefundableError;
import com.washflow.domain.usecases.serviceorder.RefundServiceOrder;
import java.util.Set;

/**
 * Validates the refund reason and delegates to {@code RefundServiceOrderRepository}, whose
 * compare-and-set only refunds orders still {@code FINALIZADO}.
 */
public class DbRefundServiceOrder implements RefundServiceOrder {

  private static final Set<String> VALID_REASONS =
      Set.of("CUSTOMER_COMPLAINT", "WRONG_CHARGE", "SERVICE_NOT_DONE", "OTHER");
  private static final int MAX_DESCRIPTION = 200;
  private static final String INVALID_REASON =
      "Escolha o motivo do estorno; em Outro, descreva em até 200 caracteres";

  private final RefundServiceOrderRepository repository;

  public DbRefundServiceOrder(RefundServiceOrderRepository repository) {
    this.repository = repository;
  }

  @Override
  public ServiceOrderRefund refund(Params params) throws ServiceOrderNotRefundableError {
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
    return repository
        .refund(
            params.serviceOrderId(),
            params.managerId(),
            reason,
            description,
            params.refundMethod(),
            params.reverseCommission())
        .orElseThrow(ServiceOrderNotRefundableError::new);
  }
}
