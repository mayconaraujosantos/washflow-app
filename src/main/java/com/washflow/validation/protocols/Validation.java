package com.washflow.validation.protocols;

import java.util.Map;

/** Returns the failure, or {@code null} when the input is valid - never throws. */
public interface Validation {

  Exception validate(Map<String, Object> input);
}
