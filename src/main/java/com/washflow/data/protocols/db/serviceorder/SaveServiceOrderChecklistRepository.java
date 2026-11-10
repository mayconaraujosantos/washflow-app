package com.washflow.data.protocols.db.serviceorder;

import com.washflow.domain.entities.ServiceOrderChecklist;

public interface SaveServiceOrderChecklistRepository {

  ServiceOrderChecklist save(ServiceOrderChecklist checklist);
}
