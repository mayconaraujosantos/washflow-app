package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrder;
import java.util.List;
import java.util.UUID;

/**
 * Returns the active service orders assigned to a specific washer. Implemented by {@code
 * DbGetWashingByWasher} in the data layer.
 */
public interface GetWashingByWasher {

  List<ServiceOrder> get(UUID washerId);
}
