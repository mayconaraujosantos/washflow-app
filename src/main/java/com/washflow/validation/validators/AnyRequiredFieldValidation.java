package com.washflow.validation.validators;

import com.washflow.presentation.errors.MissingParamError;
import com.washflow.validation.protocols.Validation;
import java.util.List;
import java.util.Map;

/** At least one of the fields must be present and non-blank (e.g. phone or e-mail). */
public class AnyRequiredFieldValidation implements Validation {

  private final List<String> fieldNames;

  public AnyRequiredFieldValidation(String... fieldNames) {
    this.fieldNames = List.of(fieldNames);
  }

  @Override
  public Exception validate(Map<String, Object> input) {
    boolean anyPresent =
        input != null
            && fieldNames.stream()
                .map(input::get)
                .anyMatch(value -> value != null && !value.toString().isBlank());
    return anyPresent ? null : new MissingParamError(String.join(" or ", fieldNames));
  }
}
