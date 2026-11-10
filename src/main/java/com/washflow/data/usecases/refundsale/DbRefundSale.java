package com.washflow.data.usecases.refundsale;

import com.washflow.data.protocols.db.RefundSaleRepository;
import com.washflow.domain.entities.Sale;
import com.washflow.domain.entities.SaleRefund;
import com.washflow.domain.errors.sale.SaleNotFoundError;
import com.washflow.domain.errors.sale.SaleRefundNotAllowedError;
import com.washflow.domain.usecases.sale.RefundSale;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;

/**
 * Validates method, amount and reason; the per-method limit is checked by {@code
 * RefundSaleRepository} under a lock, so concurrent refunds never exceed what was paid.
 */
public class DbRefundSale implements RefundSale {

  private static final Set<String> VALID_REASONS =
      Set.of("CUSTOMER_COMPLAINT", "WRONG_CHARGE", "SERVICE_NOT_DONE", "OTHER");
  private static final int MAX_DESCRIPTION = 200;

  private final RefundSaleRepository repository;
  private final Clock clock;

  public DbRefundSale(RefundSaleRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  public Sale refund(Params params) throws SaleNotFoundError, SaleRefundNotAllowedError {
    if (params.method() == null) {
      throw new IllegalArgumentException("Escolha a forma da devolução");
    }
    BigDecimal amount = params.amount();
    if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
      throw new IllegalArgumentException("Informe um valor maior que zero, com até 2 casas");
    }
    String reason = params.reason() == null ? "" : params.reason().trim();
    String description = params.description() == null ? null : params.description().trim();
    if (description != null && description.isEmpty()) {
      description = null;
    }
    if (!VALID_REASONS.contains(reason)
        || ("OTHER".equals(reason) && description == null)
        || (description != null && description.length() > MAX_DESCRIPTION)) {
      throw new IllegalArgumentException(
          "Escolha o motivo do estorno; em Outro, descreva em até 200 caracteres");
    }
    return repository.refund(
        params.saleId(),
        new SaleRefund(
            UUID.randomUUID(),
            params.saleId(),
            params.method(),
            amount.setScale(2, RoundingMode.UNNECESSARY),
            reason,
            description,
            params.managerId(),
            clock.instant()));
  }
}
