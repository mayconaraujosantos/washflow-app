package com.washflow.domain.errors.auth;

public class OtpTooSoonError extends Exception {

  public OtpTooSoonError() {
    super("Aguarde um minuto para pedir outro código");
  }
}
