package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.application.AppConfig;
import java.util.Collections;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JdbiFactoryTest {

  @Nested
  class CreateDataSource {

    @Test
    void falls_back_to_h2_when_postgres_is_unreachable_in_development() {
      AppConfig config =
          AppConfig.load(
              Collections.singletonMap("APP_ENV", "development"), Collections.emptyMap());

      DataSource dataSource = JdbiFactory.createDataSource(config);

      assertNotNull(dataSource);
    }

    @Test
    void throws_when_postgres_is_unreachable_in_production() {
      AppConfig config =
          AppConfig.load(
              java.util.Map.of(
                  "APP_ENV",
                  "production",
                  "DB_HOST",
                  "localhost",
                  "DB_USER",
                  "washflow",
                  "DB_PASSWORD",
                  "washflow",
                  "AUTH_TOKEN_SECRET",
                  "a".repeat(64)),
              Collections.emptyMap());

      IllegalStateException exception =
          assertThrows(IllegalStateException.class, () -> JdbiFactory.createDataSource(config));
      assertTrue(exception.getMessage().contains("unreachable in production"));
    }

    @Test
    void uses_h2_when_database_url_is_blank_in_development() {
      AppConfig config =
          AppConfig.load(
              Collections.singletonMap("APP_ENV", "development"), Collections.emptyMap());

      DataSource dataSource = JdbiFactory.createDataSource(config);

      assertNotNull(dataSource);
    }
  }

  @Nested
  class CreateJdbi {

    @Test
    void creates_jdbi_instance_with_sql_object_plugin() {
      AppConfig config =
          AppConfig.load(
              Collections.singletonMap("APP_ENV", "development"), Collections.emptyMap());

      Jdbi jdbi = JdbiFactory.create(config);

      assertNotNull(jdbi);
    }
  }
}
