package com.washflow.data.usecases.savechecklist;

import com.washflow.data.protocols.db.SaveServiceOrderChecklistRepository;
import com.washflow.domain.entities.ServiceOrderChecklist;
import com.washflow.domain.errors.InvalidChecklistTypeError;
import com.washflow.domain.usecases.SaveServiceOrderChecklist;
import java.time.Instant;
import java.util.UUID;

/**
 * Validates checklist params and persists via {@code SaveServiceOrderChecklistRepository}. Depends
 * only on the port interface, never on JDBI or Postgres.
 */
public class DbSaveServiceOrderChecklist implements SaveServiceOrderChecklist {

  private final SaveServiceOrderChecklistRepository saveRepository;

  public DbSaveServiceOrderChecklist(SaveServiceOrderChecklistRepository saveRepository) {
    this.saveRepository = saveRepository;
  }

  @Override
  public ServiceOrderChecklist save(Params params) throws InvalidChecklistTypeError {
    if (params.type() == null) {
      throw new InvalidChecklistTypeError(null);
    }

    if (params.items() == null || params.items().isEmpty()) {
      throw new InvalidChecklistTypeError("items cannot be empty");
    }

    ServiceOrderChecklist checklist =
        new ServiceOrderChecklist(
            UUID.randomUUID(),
            params.serviceOrderId(),
            params.type(),
            params.items(),
            params.signatureImage(),
            Instant.now(),
            params.createdBy());

    return saveRepository.save(checklist);
  }
}
