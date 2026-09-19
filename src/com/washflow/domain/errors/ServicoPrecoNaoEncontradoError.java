package com.washflow.domain.errors;

public class ServicoPrecoNaoEncontradoError extends Exception {

  public ServicoPrecoNaoEncontradoError(int servicoPrecoId) {
    super("Serviço não encontrado: " + servicoPrecoId);
  }
}
