package com.washflow.domain.errors.common;

/**
 * A request touched establishment data without an establishment: a public screen that did not say
 * which lava-jato it is for, or a code path that forgot to run inside one. Answered as 400.
 */
public class TenantRequiredError extends RuntimeException {

  public TenantRequiredError() {
    super("Escolha o lava-jato");
  }
}
