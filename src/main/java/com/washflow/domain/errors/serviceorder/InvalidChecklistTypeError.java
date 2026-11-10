package com.washflow.domain.errors.serviceorder;

public class InvalidChecklistTypeError extends Exception {

  public InvalidChecklistTypeError(String value) {
    super("Invalid checklist type: " + value + ". Must be ENTRY or EXIT");
  }
}
