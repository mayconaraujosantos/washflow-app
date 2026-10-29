package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;
import java.util.List;
import java.util.UUID;

/** Port for loading the active service orders assigned to a specific washer. */
public interface LoadWashingByWasherRepository {

  List<ServiceOrder> loadByWasherId(UUID washerId);
}
