package com.washflow.application.factories.controllers.schedule;

import com.washflow.application.factories.usecases.schedule.ListAgendaFactory;
import com.washflow.presentation.controllers.listagenda.ListAgendaController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListAgendaControllerFactory {

  private ListAgendaControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListAgendaController(ListAgendaFactory.make(jdbi));
  }
}
