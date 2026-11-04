package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.listagenda.DbListAgenda;
import com.washflow.domain.usecases.ListAgenda;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ListAgendaFactory {

  private ListAgendaFactory() {}

  public static ListAgenda make(Jdbi jdbi) {
    return new DbListAgenda(new ServiceOrderJdbiRepository(jdbi));
  }
}
