package com.washflow.application.factories.controllers.vehicle;

import com.washflow.application.factories.usecases.vehicle.SearchVehiclesFactory;
import com.washflow.presentation.controllers.vehicle.SearchVehiclesController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class SearchVehiclesControllerFactory {

  private SearchVehiclesControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new SearchVehiclesController(SearchVehiclesFactory.make(jdbi));
  }
}
