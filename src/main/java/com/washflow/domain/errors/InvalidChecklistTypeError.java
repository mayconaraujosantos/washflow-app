package com.washflow.domain.errors;

public class InvalidChecklistTypeError extends Exception {

  public InvalidChecklistTypeError(String value) {
    super("Invalid checklist type: " + value + ". Must be ENTRY or EXIT");
  }
}
