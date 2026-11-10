package com.washflow.domain.usecases.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrderChecklist;
import com.washflow.domain.entities.serviceorder.ServiceOrderChecklist.ChecklistItem;
import com.washflow.domain.entities.serviceorder.ServiceOrderChecklist.ChecklistType;
import com.washflow.domain.errors.serviceorder.InvalidChecklistTypeError;
import java.util.List;
import java.util.UUID;

/**
 * Saves a checklist for a service order. The type must be ENTRY or EXIT, and items must not be
 * empty. Implemented by {@code DbSaveServiceOrderChecklist} in the data layer.
 */
public interface SaveServiceOrderChecklist {

  ServiceOrderChecklist save(Params params) throws InvalidChecklistTypeError;

  record Params(
      UUID serviceOrderId,
      ChecklistType type,
      List<ChecklistItem> items,
      String signatureImage,
      UUID createdBy) {}
}
