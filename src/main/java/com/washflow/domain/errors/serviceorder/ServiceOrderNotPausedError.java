package com.washflow.domain.errors.serviceorder;

public class ServiceOrderNotPausedError extends Exception {

  public ServiceOrderNotPausedError() {
    super("Service order is not an active wash assigned to you");
  }
}
