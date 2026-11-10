package com.washflow.data.protocols.db.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrderChecklist;

public interface SaveServiceOrderChecklistRepository {

  ServiceOrderChecklist save(ServiceOrderChecklist checklist);
}
