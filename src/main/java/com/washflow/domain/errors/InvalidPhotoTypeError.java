package com.washflow.domain.errors;

public class InvalidPhotoTypeError extends Exception {

  public InvalidPhotoTypeError(String value) {
    super("Invalid photo type: " + value + ". Must be ENTRY or EXIT");
  }
}
