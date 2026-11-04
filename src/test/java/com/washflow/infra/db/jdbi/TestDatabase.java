package com.washflow.infra.db.jdbi;

import com.washflow.data.tenancy.TenantContext;
import com.washflow.infra.db.tenancy.TenantBindingCustomizer;
import java.util.UUID;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlStatements;

/**
 * Integration-test database: the migrated H2 schema, the production tenant binding, and the demo
 * fixtures the tests refer to (customer 1111..., vehicle 2222..., washer 3333..., manager 4444...,
 * service 1) inside one test lava-jato - which becomes the current establishment of the test
 * thread.
 */
public final class TestDatabase {

  public static final UUID ESTABLISHMENT_ID =
      UUID.fromString("eeeeeeee-eeee-eeee-eeee-000000000001");
  public static final String ESTABLISHMENT_SLUG = "lava-teste";

  private TestDatabase() {}

  public static Jdbi prepare(DataSource dataSource) {
    Jdbi jdbi = Jdbi.create(dataSource);
    jdbi.getConfig(SqlStatements.class).addCustomizer(new TenantBindingCustomizer());
    jdbi.useHandle(
        handle -> {
          handle
              .createUpdate(
                  """
                  INSERT INTO estabelecimentos (id, slug, nome, hora_abertura, hora_fechamento,
                    abre_domingo, max_veiculos_hora, ativo)
                  VALUES (:tenant, :slug, 'Lava Teste', 8, 18, FALSE, 3, TRUE)
                  """)
              .bind("tenant", ESTABLISHMENT_ID)
              .bind("slug", ESTABLISHMENT_SLUG)
              .execute();
          user(
              handle,
              "11111111-1111-1111-1111-111111111111",
              "Cliente Demo",
              "11999990000",
              "CLIENTE");
          user(
              handle,
              "33333333-3333-3333-3333-333333333333",
              "Lavador Demo",
              "11999991111",
              "LAVADOR");
          user(
              handle,
              "44444444-4444-4444-4444-444444444444",
              "Gerente Demo",
              "11999993333",
              "GERENTE");
          handle
              .createUpdate(
                  """
                  INSERT INTO veiculos (id, estabelecimento_id, cliente_id, placa, modelo, cor)
                  VALUES ('22222222-2222-2222-2222-222222222222', :tenant,
                          '11111111-1111-1111-1111-111111111111', 'ABC1D23', 'Onix', 'Prata')
                  """)
              .bind("tenant", ESTABLISHMENT_ID)
              .execute();
          handle
              .createUpdate(
                  """
                  INSERT INTO servicos_preco (id, estabelecimento_id, nome, preco, comissao_lavador)
                  VALUES (1, :tenant, 'Lavagem Completa', 60.00, 20.00)
                  """)
              .bind("tenant", ESTABLISHMENT_ID)
              .execute();
        });
    TenantContext.set(ESTABLISHMENT_ID);
    return jdbi;
  }

  private static void user(
      org.jdbi.v3.core.Handle handle, String id, String name, String phone, String profile) {
    handle
        .createUpdate(
            """
            INSERT INTO usuarios (id, estabelecimento_id, nome, telefone, perfil)
            VALUES (CAST(:id AS UUID), :tenant, :name, :phone, :profile)
            """)
        .bind("id", id)
        .bind("tenant", ESTABLISHMENT_ID)
        .bind("name", name)
        .bind("phone", phone)
        .bind("profile", profile)
        .execute();
  }
}
