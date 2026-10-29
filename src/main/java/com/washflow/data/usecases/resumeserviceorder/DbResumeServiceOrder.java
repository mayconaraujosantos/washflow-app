package com.washflow.data.usecases.resumeserviceorder;

import com.washflow.data.protocols.db.ResumeServiceOrderRepository;
import com.washflow.domain.errors.ServiceOrderNotWashingError;
import com.washflow.domain.usecases.ResumeServiceOrder;

/**
 * Delegates to {@code ResumeServiceOrderRepository}. Depends only on the port interface, never on
 * JDBI or Postgres.
 */
public class DbResumeServiceOrder implements ResumeServiceOrder {

  private final ResumeServiceOrderRepository resumeRepository;

  public DbResumeServiceOrder(ResumeServiceOrderRepository resumeRepository) {
    this.resumeRepository = resumeRepository;
  }

  @Override
  public void resume(Params params) throws ServiceOrderNotWashingError {
    boolean resumed = resumeRepository.resume(params.serviceOrderId(), params.washerId());
    if (!resumed) {
      throw new ServiceOrderNotWashingError();
    }
  }
}
