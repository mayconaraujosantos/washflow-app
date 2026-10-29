package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.savechecklist.DbSaveServiceOrderChecklist;
import com.washflow.domain.usecases.SaveServiceOrderChecklist;
import com.washflow.infra.db.jdbi.ChecklistJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new
 * ChecklistJdbiRepository(...)} call here - {@code DbSaveServiceOrderChecklist} depends on the
 * {@code data.protocols.db} interfaces, not on this concrete class.
 */
public final class SaveServiceOrderChecklistFactory {

  private SaveServiceOrderChecklistFactory() {}

  public static SaveServiceOrderChecklist make(Jdbi jdbi) {
    ChecklistJdbiRepository repository = new ChecklistJdbiRepository(jdbi);
    return new DbSaveServiceOrderChecklist(repository);
  }
}
