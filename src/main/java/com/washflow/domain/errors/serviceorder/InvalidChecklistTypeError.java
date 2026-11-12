package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.ValidationError;

public class InvalidChecklistTypeError extends ValidationError {

  public InvalidChecklistTypeError(String value) {
    super("INVALID_CHECKLIST_TYPE", "Invalid checklist type: " + value + ". Must be ENTRY or EXIT");
  }
}
