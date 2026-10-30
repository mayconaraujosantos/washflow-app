package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrderChecklist;
import java.util.List;
import java.util.UUID;

/**
 * Lists all checklists for a given service order. Implemented by {@code
 * DbListServiceOrderChecklists} in the data layer.
 */
public interface ListServiceOrderChecklists {

  List<ServiceOrderChecklist> list(UUID serviceOrderId);
}
