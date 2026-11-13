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

  @Test
  void emailChannelFollowsSmtpHostUnlessSetAndNeedsTheSender() {
    AppConfig dev = AppConfig.load(Map.of("APP_ENV", "development"), Map.of());
    assertEquals("log", dev.emailChannel());

    AppConfig prod =
        AppConfig.load(
            Map.of(
                "APP_ENV", "production",
                "DATABASE_URL", "postgres://u:p@db/x",
                "AUTH_TOKEN_SECRET", "0123456789abcdef0123456789abcdef"),
            Map.of());
    assertEquals("none", prod.emailChannel());

    AppConfig brevo =
        AppConfig.load(
            Map.of(
                "SMTP_HOST", "smtp-relay.brevo.com",
                "SMTP_USERNAME", "user",
                "SMTP_PASSWORD", "segredo",
                "SMTP_FROM", "nao-responda@washflow.app"),
            Map.of());
    assertEquals("smtp", brevo.emailChannel());
    assertEquals("smtp-relay.brevo.com", brevo.smtp().host());
    assertEquals(587, brevo.smtp().port());
    assertEquals("starttls", brevo.smtp().security());
    assertEquals("Washflow", brevo.smtp().fromName());
    assertFalse(brevo.smtp().toString().contains("segredo"));

    AppConfig mailpit =
        AppConfig.load(
            Map.of(
                "SMTP_HOST", "127.0.0.1",
                "SMTP_PORT", "1025",
                "SMTP_SECURITY", "NONE",
                "SMTP_FROM", "teste@washflow.test"),
            Map.of());
    assertEquals("none", mailpit.smtp().security());

    AppConfig logged =
        AppConfig.load(Map.of("EMAIL_CHANNEL", "log", "SMTP_HOST", "127.0.0.1"), Map.of());
    assertEquals("log", logged.emailChannel());

    assertThrows(
        IllegalStateException.class,
        () -> AppConfig.load(Map.of("SMTP_HOST", "smtp.example.com"), Map.of()));
    assertThrows(
        IllegalStateException.class,
        () -> AppConfig.load(Map.of("EMAIL_CHANNEL", "pombo"), Map.of()));
    assertThrows(
        IllegalStateException.class,
        () ->
            AppConfig.load(
                Map.of("SMTP_HOST", "h", "SMTP_FROM", "a@b.co", "SMTP_SECURITY", "tls"), Map.of()));
  }

  @Test
  void evolutionWhatsappChannelNeedsTheServerKeyAndInstance() {
    AppConfig config =
        AppConfig.load(
            Map.of(
                "OTP_CHANNEL", "Evolution",
                "EVOLUTION_URL", "http://evolution:8080",
                "EVOLUTION_API_KEY", "secret-key",
                "EVOLUTION_INSTANCE", "washflow"),
            Map.of());

    assertEquals("evolution", config.otpChannel());
    assertEquals("http://evolution:8080", config.evolution().url());
    assertFalse(config.toString().contains("secret-key"));
    assertThrows(
        IllegalStateException.class,
        () -> AppConfig.load(Map.of("OTP_CHANNEL", "evolution"), Map.of()));
    assertThrows(
        IllegalStateException.class, () -> AppConfig.load(Map.of("OTP_CHANNEL", "zapi"), Map.of()));
  }
}
