package com.washflow.application.factories.controllers.vehicle;

import com.washflow.application.factories.usecases.vehicle.ListVehiclesByCustomerFactory;
import com.washflow.presentation.controllers.vehicle.ListVehiclesByCustomerController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListVehiclesByCustomerControllerFactory {

  private ListVehiclesByCustomerControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListVehiclesByCustomerController(ListVehiclesByCustomerFactory.make(jdbi));
  }
}
