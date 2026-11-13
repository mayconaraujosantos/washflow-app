package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

/** V14 rewrites the phones stored before every path normalized them. */
class CanonicalPhonesMigrationIntegrationTest {

  private static final UUID SHOP = UUID.randomUUID();

  @Test
  void storedPhonesTakeTheOneSpellingUnlessThatMakesTwoAccountsOne() {
    DataSource dataSource = TestDatabase.emptyDatabase();
    flyway(dataSource).target("13").load().migrate();
    Jdbi jdbi = Jdbi.create(dataSource);
    jdbi.useHandle(
        handle -> {
          handle.execute(
              """
              INSERT INTO estabelecimentos (id, slug, nome, hora_abertura, hora_fechamento,
                abre_domingo, max_veiculos_hora, ativo)
              VALUES (?, 'lava', 'Lava', 8, 18, FALSE, 3, TRUE)
              """,
              SHOP);
          user(handle, "customer", "92986061046", "CLIENTE");
          // The same person registered as washer with +55 and without the ninth digit.
          user(handle, "same-person", "559286061046", "LAVADOR");
          user(handle, "washer", "559292569437", "LAVADOR");
          user(handle, "landline", "9233334444", "CLIENTE");
          handle.execute(
              """
              INSERT INTO veiculos (id, estabelecimento_id, placa, contato_telefone)
              VALUES (?, ?, 'ABC1D23', '+55 (92) 8111-2222')
              """,
              UUID.randomUUID(),
              SHOP);
        });

    flyway(dataSource).load().migrate();

    assertEquals("92986061046", phoneOf(jdbi, "customer"));
    // Would collide with the customer: left for a person to decide which account stays.
    assertEquals("559286061046", phoneOf(jdbi, "same-person"));
    assertEquals("92992569437", phoneOf(jdbi, "washer"));
    assertEquals("9233334444", phoneOf(jdbi, "landline"));
    assertEquals(
        "92981112222",
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT contato_telefone FROM veiculos")
                    .mapTo(String.class)
                    .one()));
  }

  private static org.flywaydb.core.api.configuration.FluentConfiguration flyway(
      DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .table("schema_history")
        .baselineOnMigrate(true)
        .baselineVersion("0");
  }

  private static void user(org.jdbi.v3.core.Handle handle, String name, String phone, String role) {
    handle.execute(
        """
        INSERT INTO usuarios (id, estabelecimento_id, nome, telefone, perfil)
        VALUES (?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        SHOP,
        name,
        phone,
        role);
  }

  private static String phoneOf(Jdbi jdbi, String name) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT telefone FROM usuarios WHERE nome = :name")
                .bind("name", name)
                .mapTo(String.class)
                .one());
  }
}
