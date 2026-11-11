package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.ValidationError;

public class InvalidPhotoTypeError extends ValidationError {

  public InvalidPhotoTypeError(String value) {
    super("INVALID_PHOTO_TYPE", "Invalid photo type: " + value + ". Must be ENTRY or EXIT");
  }
}
