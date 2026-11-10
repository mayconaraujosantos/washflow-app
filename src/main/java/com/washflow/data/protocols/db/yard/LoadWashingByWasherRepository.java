package com.washflow.data.protocols.db.yard;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.util.List;
import java.util.UUID;

/** Port for loading the active service orders assigned to a specific washer. */
public interface LoadWashingByWasherRepository {

  List<ServiceOrder> loadByWasherId(UUID washerId);
}
