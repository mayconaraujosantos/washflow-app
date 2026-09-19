package com.washflow.validation.validators;

import com.washflow.validation.protocols.Validation;
import java.util.List;
import java.util.Map;

public class ValidationComposite implements Validation {

  private final List<Validation> validations;

  public ValidationComposite(List<Validation> validations) {
    this.validations = validations;
  }

  @Override
  public Exception validate(Map<String, Object> input) {
    for (Validation validation : validations) {
      Exception error = validation.validate(input);
      if (error != null) {
        return error;
      }
    }
    return null;
  }
}
