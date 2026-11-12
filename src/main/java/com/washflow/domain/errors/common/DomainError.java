package com.washflow.domain.errors.common;

/**
 * A business rule the request broke. Each error belongs to one category, which says what kind of
 * failure it is (invalid input, not found, conflict...) without knowing about HTTP; the
 * presentation layer turns the category into a status code in a single place.
 *
 * <p>{@link #code()} is a stable identifier ({@code SERVICE_ORDER_NOT_CANCELLABLE}) for clients to
 * react to, while the message is the text shown to the user.
 */
public abstract sealed class DomainError extends RuntimeException
    permits ValidationError,
        AuthenticationError,
        AccessDeniedError,
        NotFoundError,
        ConflictError,
        TooManyRequestsError,
        UnavailableError {

  private final String code;

  protected DomainError(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
