package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrderChecklist;

public interface SaveServiceOrderChecklistRepository {

  ServiceOrderChecklist save(ServiceOrderChecklist checklist);
}
