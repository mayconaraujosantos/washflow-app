package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.listchecklists.DbListServiceOrderChecklists;
import com.washflow.domain.usecases.ListServiceOrderChecklists;
import com.washflow.infra.db.jdbi.ChecklistJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new
 * ChecklistJdbiRepository(...)} call here - {@code DbListServiceOrderChecklists} depends on the
 * {@code data.protocols.db} interfaces, not on this concrete class.
 */
public final class ListServiceOrderChecklistsFactory {

  private ListServiceOrderChecklistsFactory() {}

  public static ListServiceOrderChecklists make(Jdbi jdbi) {
    ChecklistJdbiRepository repository = new ChecklistJdbiRepository(jdbi);
    return new DbListServiceOrderChecklists(repository);
  }
}
