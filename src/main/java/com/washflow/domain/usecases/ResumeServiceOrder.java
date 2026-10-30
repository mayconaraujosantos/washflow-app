package com.washflow.domain.usecases;

import com.washflow.domain.errors.ServiceOrderNotWashingError;
import java.util.UUID;

/** Resumes a paused wash. Implemented by {@code DbResumeServiceOrder} in the data layer. */
public interface ResumeServiceOrder {

  void resume(Params params) throws ServiceOrderNotWashingError;

  record Params(UUID serviceOrderId, UUID washerId) {}
}
