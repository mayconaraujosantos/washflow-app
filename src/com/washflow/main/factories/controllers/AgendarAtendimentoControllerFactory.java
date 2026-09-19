package com.washflow.main.factories.controllers;

import com.washflow.main.factories.usecases.AgendarAtendimentoFactory;
import com.washflow.presentation.controllers.agendaratendimento.AgendarAtendimentoController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

public final class AgendarAtendimentoControllerFactory {

  private AgendarAtendimentoControllerFactory() {}

  public static Controller make(Jdbi jdbi) {
    Validation validation =
        new ValidationComposite(
            List.of(
                new RequiredFieldValidation("veiculoId"),
                new RequiredFieldValidation("servicoPrecoId"),
                new RequiredFieldValidation("dataAgendamento")));

    return new AgendarAtendimentoController(AgendarAtendimentoFactory.make(jdbi), validation);
  }
}
