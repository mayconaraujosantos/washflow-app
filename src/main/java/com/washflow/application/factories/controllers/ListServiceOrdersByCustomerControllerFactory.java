package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.ListServiceOrdersByCustomerFactory;
import com.washflow.presentation.controllers.listserviceordersbycustomer.ListServiceOrdersByCustomerController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListServiceOrdersByCustomerControllerFactory {

  private ListServiceOrdersByCustomerControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListServiceOrdersByCustomerController(ListServiceOrdersByCustomerFactory.make(jdbi));
  }
}
