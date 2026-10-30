package com.washflow.data.usecases.pauseserviceorder;

import com.washflow.data.protocols.db.PauseServiceOrderRepository;
import com.washflow.domain.errors.ServiceOrderNotPausedError;
import com.washflow.domain.usecases.PauseServiceOrder;
import java.util.Set;

/**
 * Validates pause params and delegates to {@code PauseServiceOrderRepository}. Depends only on the
 * port interface, never on JDBI or Postgres.
 */
public class DbPauseServiceOrder implements PauseServiceOrder {

  private static final Set<String> VALID_REASONS = Set.of("LUNCH", "RESTROOM", "OTHER");

  private final PauseServiceOrderRepository pauseRepository;

  public DbPauseServiceOrder(PauseServiceOrderRepository pauseRepository) {
    this.pauseRepository = pauseRepository;
  }

  @Override
  public void pause(Params params) throws ServiceOrderNotPausedError {
    String reason = params.reason() == null ? "" : params.reason().trim();
    String description = params.description() == null ? null : params.description().trim();

    if (!VALID_REASONS.contains(reason)) {
      throw new IllegalArgumentException(
          "Choose a valid pause reason; OTHER requires a description of up to 200 characters");
    }
    if ("OTHER".equals(reason) && (description == null || description.isBlank())) {
      throw new IllegalArgumentException(
          "Choose a valid pause reason; OTHER requires a description of up to 200 characters");
    }
    if (description != null && description.length() > 200) {
      throw new IllegalArgumentException(
          "Choose a valid pause reason; OTHER requires a description of up to 200 characters");
    }
    if (description != null && description.isBlank()) {
      description = null;
    }

    boolean paused =
        pauseRepository.pause(params.serviceOrderId(), params.washerId(), reason, description);
    if (!paused) {
      throw new ServiceOrderNotPausedError();
    }
  }
}
