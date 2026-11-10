package com.washflow.application.factories.usecases.schedule;

import com.washflow.data.usecases.schedule.DbListAgenda;
import com.washflow.domain.usecases.schedule.ListAgenda;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ListAgendaFactory {

  private ListAgendaFactory() {}

  public static ListAgenda make(Jdbi jdbi) {
    return new DbListAgenda(new ServiceOrderJdbiRepository(jdbi));
  }
}
