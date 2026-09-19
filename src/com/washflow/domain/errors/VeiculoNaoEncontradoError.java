package com.washflow.domain.errors;

import java.util.UUID;

public class VeiculoNaoEncontradoError extends Exception {

  public VeiculoNaoEncontradoError(UUID veiculoId) {
    super("Veículo não encontrado: " + veiculoId);
  }
}
