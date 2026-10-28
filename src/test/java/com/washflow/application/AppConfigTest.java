package com.washflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AppConfigTest {

  @Test
  void processEnvironmentOverridesDotenvValues() {
    AppConfig config =
        AppConfig.load(
            Map.of("PORT", "8081", "DB_PASSWORD", "process-secret"),
            Map.of("PORT", "7001", "DB_PASSWORD", "dotenv-secret"));

    assertEquals(8081, config.port());
    assertEquals("process-secret", config.databasePassword());
  }

  @Test
  void dotenvValuesAreUsedOutsideProduction() {
    AppConfig config =
        AppConfig.load(
            Map.of(),
            Map.of(
                "APP_ENV", "development",
                "PORT", "7001",
                "DB_HOST", "localhost",
                "MAX_VEHICLES_PER_HOUR", "5"));

    assertEquals("development", config.environment());
    assertEquals(7001, config.port());
    assertEquals("localhost", config.databaseHost());
    assertEquals(5, config.maxVehiclesPerHour());
    assertFalse(config.isProduction());
  }

  @Test
  void productionDoesNotReadDotenvAndRequiresDatabaseConfiguration() {
    assertThrows(
        IllegalStateException.class,
        () ->
            AppConfig.load(
                Map.of(
                    "APP_ENV",
                    "production",
                    "AUTH_TOKEN_SECRET",
                    "0123456789abcdef0123456789abcdef"),
                Map.of("DATABASE_URL", "postgres://user:password@db:5432/washflow")));
  }

  @Test
  void productionAcceptsDatabaseUrlFromProcessEnvironment() {
    AppConfig config =
        AppConfig.load(
            Map.of(
                "APP_ENV",
                "production",
                "AUTH_TOKEN_SECRET",
                "0123456789abcdef0123456789abcdef",
                "DATABASE_URL",
                "postgres://user:password@db:5432/washflow"),
            Map.of());

    assertTrue(config.isProduction());
    assertEquals("postgres://user:password@db:5432/washflow", config.databaseUrl());
  }

  @Test
  void invalidIntegerConfigurationFailsWithSettingName() {
    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> AppConfig.load(Map.of("PORT", "not-a-port"), Map.of()));

    assertTrue(error.getMessage().contains("PORT"));
  }
}
