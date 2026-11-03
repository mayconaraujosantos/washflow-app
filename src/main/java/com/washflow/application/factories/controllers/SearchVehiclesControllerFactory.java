package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.SearchVehiclesFactory;
import com.washflow.presentation.controllers.searchvehicles.SearchVehiclesController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class SearchVehiclesControllerFactory {

  private SearchVehiclesControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new SearchVehiclesController(SearchVehiclesFactory.make(jdbi));
  }
}
