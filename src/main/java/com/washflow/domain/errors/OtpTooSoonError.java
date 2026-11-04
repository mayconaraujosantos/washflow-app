package com.washflow.domain.errors;

public class OtpTooSoonError extends Exception {

  public OtpTooSoonError() {
    super("Aguarde um minuto para pedir outro código");
  }
}
