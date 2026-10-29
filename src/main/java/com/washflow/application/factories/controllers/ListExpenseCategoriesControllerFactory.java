package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.ListExpenseCategoriesFactory;
import com.washflow.presentation.controllers.listexpensecategories.ListExpenseCategoriesController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class ListExpenseCategoriesControllerFactory {

  private ListExpenseCategoriesControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new ListExpenseCategoriesController(ListExpenseCategoriesFactory.make(jdbi));
  }
}
