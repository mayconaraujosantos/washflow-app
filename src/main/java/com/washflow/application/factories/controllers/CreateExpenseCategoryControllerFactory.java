package com.washflow.application.factories.controllers;

import com.washflow.application.factories.usecases.CreateExpenseCategoryFactory;
import com.washflow.presentation.controllers.createexpensecategory.CreateExpenseCategoryController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

public final class CreateExpenseCategoryControllerFactory {

  private CreateExpenseCategoryControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    return new CreateExpenseCategoryController(CreateExpenseCategoryFactory.make(jdbi));
  }
}
