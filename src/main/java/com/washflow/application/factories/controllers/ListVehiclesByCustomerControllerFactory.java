package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.ListVehiclesByCustomerFactory;
import com.washflow.presentation.controllers.listvehiclesbycustomer.ListVehiclesByCustomerController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListVehiclesByCustomerControllerFactory {

  private ListVehiclesByCustomerControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListVehiclesByCustomerController(ListVehiclesByCustomerFactory.make(jdbi));
  }
}
