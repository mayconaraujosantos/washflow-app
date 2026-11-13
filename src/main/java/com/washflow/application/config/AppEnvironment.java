package com.washflow.application.config;

/** APP_ENV values: anything other than "production" (any case) runs as development. */
public final class AppEnvironment {

  public static final String DEVELOPMENT = "development";
  public static final String PRODUCTION = "production";

  private AppEnvironment() {}

  public static boolean isProduction(String environment) {
    return PRODUCTION.equalsIgnoreCase(environment);
  }
}
