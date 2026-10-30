package com.washflow.validation.validators;

import com.washflow.presentation.errors.MissingParamError;
import com.washflow.validation.protocols.Validation;
import java.util.Map;

public class RequiredFieldValidation implements Validation {

  private final String fieldName;

  public RequiredFieldValidation(String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public Exception validate(Map<String, Object> input) {
    if (input.get(fieldName) == null) {
      return new MissingParamError(fieldName);
    }
    return null;
  }
}
