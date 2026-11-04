package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.application.AppConfig;
import java.util.Map;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JdbiFactoryTest {

  @ParameterizedTest
  @ValueSource(strings = {"development", "production"})
  void refusesToStartWithoutAReachablePostgres(String environment) {
    AppConfig config =
        AppConfig.load(
            Map.of(
                "APP_ENV", environment,
                "DB_HOST", "127.0.0.1",
                "DB_PORT", "1",
                "DB_USER", "washflow",
                "DB_PASSWORD", "washflow",
                "AUTH_TOKEN_SECRET", "a".repeat(64)),
            Map.of());

    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> JdbiFactory.createDataSource(config));

    assertTrue(error.getMessage().contains("unreachable"), error.getMessage());
  }

  @Test
  void connectsMigratesAndBindsTheTenant() {
    String database = TestDatabase.createEmptyDatabase();
    AppConfig config =
        AppConfig.load(
            Map.of(
                "APP_ENV", "development",
                "DB_HOST", "localhost",
                "DB_PORT", String.valueOf(TestDatabase.port()),
                "DB_NAME", database,
                "DB_USER", "postgres",
                "DB_PASSWORD", "postgres"),
            Map.of());

    Jdbi jdbi = JdbiFactory.create(config);

    int establishments =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT COUNT(*) FROM estabelecimentos")
                    .mapTo(Integer.class)
                    .one());
    assertEquals(0, establishments);
  }
}
