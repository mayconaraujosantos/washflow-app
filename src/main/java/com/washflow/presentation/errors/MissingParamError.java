package com.washflow.presentation.errors;

public class MissingParamError extends Exception {

  public MissingParamError(String paramName) {
    super("Missing param: " + paramName);
  }
}
