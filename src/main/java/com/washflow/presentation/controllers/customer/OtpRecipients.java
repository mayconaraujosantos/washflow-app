package com.washflow.presentation.controllers.customer;

import com.washflow.domain.entities.customer.OtpRecipient;
import com.washflow.presentation.helpers.BodyValues;
import java.util.Map;

/**
 * Reads the code's recipient from the body: {@code email} when present, otherwise {@code phone}.
 */
final class OtpRecipients {

  private OtpRecipients() {}

  static OtpRecipient from(Map<String, Object> body) {
    String email = BodyValues.text(body, "email");
    if (email != null && !email.isBlank()) {
      return OtpRecipient.email(email);
    }
    return OtpRecipient.phone(BodyValues.text(body, "phone"));
  }
}
