package com.washflow.domain.usecases;

import com.washflow.domain.errors.ServiceOrderNotPausedError;
import java.util.UUID;

/**
 * Pauses an active wash, recording the reason and optional description. Implemented by {@code
 * DbPauseServiceOrder} in the data layer.
 */
public interface PauseServiceOrder {

  void pause(Params params) throws ServiceOrderNotPausedError;

  record Params(UUID serviceOrderId, UUID washerId, String reason, String description) {}
}
