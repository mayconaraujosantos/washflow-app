package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrderChecklist;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadServiceOrderChecklistRepository {

  List<ServiceOrderChecklist> loadByServiceOrderId(UUID serviceOrderId);

  Optional<ServiceOrderChecklist> loadByServiceOrderIdAndType(
      UUID serviceOrderId, ServiceOrderChecklist.ChecklistType type);
}
