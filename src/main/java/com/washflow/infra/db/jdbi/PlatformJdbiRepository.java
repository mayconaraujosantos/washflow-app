package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.PlatformRepository;
import com.washflow.domain.entities.Establishment;
import com.washflow.domain.entities.EstablishmentListing;
import com.washflow.domain.entities.EstablishmentSummary;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

public class PlatformJdbiRepository implements PlatformRepository {

  /** Starter catalog of every new lava-jato; the manager edits prices afterwards. */
  private static final List<Object[]> STARTER_SERVICES =
      List.of(
          new Object[] {"Lavagem Simples", "35.00", "10.00"},
          new Object[] {"Lavagem Completa", "60.00", "20.00"},
          new Object[] {"Lavagem Premium", "80.00", "25.00"},
          new Object[] {"Lavagem Interna", "25.00", "8.00"},
          new Object[] {"Lavagem Externa", "30.00", "10.00"},
          new Object[] {"Lavagem + Wax", "90.00", "30.00"},
          new Object[] {"Polimento", "120.00", "35.00"},
          new Object[] {"Higienização", "150.00", "40.00"});

  private static final List<String[]> STARTER_EXPENSE_CATEGORIES =
      List.of(
          new String[] {"Produtos", "#EF4444"},
          new String[] {"Manutenção", "#F59E0B"},
          new String[] {"Aluguel", "#3B82F6"},
          new String[] {"Energia", "#10B981"},
          new String[] {"Água", "#06B6D4"},
          new String[] {"Salários", "#8B5CF6"},
          new String[] {"Outros", "#6B7280"});

  private static final String SUMMARY_SQL =
      """
      SELECT e.id, e.slug, e.nome, e.ativo, e.criado_em,
             (SELECT u.nome FROM usuarios u WHERE u.estabelecimento_id = e.id
                AND u.perfil = 'GERENTE' ORDER BY u.criado_em LIMIT 1) AS manager_name,
             (SELECT u.email FROM usuarios u WHERE u.estabelecimento_id = e.id
                AND u.perfil = 'GERENTE' ORDER BY u.criado_em LIMIT 1) AS manager_email
      FROM estabelecimentos e
      """;

  private final Jdbi jdbi;

  public PlatformJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public boolean adminExists() {
    return jdbi.withHandle(
        handle ->
            handle.createQuery("SELECT COUNT(*) FROM administradores").mapTo(Integer.class).one()
                > 0);
  }

  @Override
  public boolean createFirstAdmin(User admin, String passwordHash) {
    return jdbi.inTransaction(
        handle -> {
          int existing =
              handle.createQuery("SELECT COUNT(*) FROM administradores").mapTo(Integer.class).one();
          if (existing > 0) {
            return false;
          }
          handle
              .createUpdate(
                  """
                  INSERT INTO administradores (id, nome, email, senha_hash, criado_em)
                  VALUES (:id, :name, :email, :hash, :createdAt)
                  """)
              .bind("id", admin.id())
              .bind("name", admin.name())
              .bind("email", admin.email())
              .bind("hash", passwordHash)
              .bind("createdAt", admin.createdAt())
              .execute();
          return true;
        });
  }

  @Override
  public Optional<User> loadAdminByEmail(String email) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT id, nome, email, criado_em FROM administradores WHERE email = :email")
                .bind("email", email)
                .map(
                    (rs, ctx) ->
                        new User(
                            rs.getObject("id", UUID.class),
                            rs.getString("nome"),
                            null,
                            UserProfile.ADMIN,
                            rs.getTimestamp("criado_em").toInstant(),
                            true,
                            rs.getString("email")))
                .findOne());
  }

  @Override
  public Optional<String> loadAdminPasswordHash(UUID adminId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT senha_hash FROM administradores WHERE id = :id")
                .bind("id", adminId)
                .mapTo(String.class)
                .findOne());
  }

  @Override
  public boolean slugExists(String slug) {
    return jdbi.withHandle(
        handle ->
            handle
                    .createQuery("SELECT COUNT(*) FROM estabelecimentos WHERE slug = :slug")
                    .bind("slug", slug)
                    .mapTo(Integer.class)
                    .one()
                > 0);
  }

  @Override
  public boolean emailInUse(String email) {
    return jdbi.withHandle(
        handle ->
            handle
                    .createQuery(
                        """
                        SELECT (SELECT COUNT(*) FROM usuarios WHERE email = :email)
                             + (SELECT COUNT(*) FROM administradores WHERE email = :email)
                        """)
                    .bind("email", email)
                    .mapTo(Integer.class)
                    .one()
                > 0);
  }

  @Override
  public EstablishmentSummary createEstablishment(
      UUID id, String slug, Establishment settings, User manager, String managerPasswordHash) {
    return jdbi.inTransaction(
        handle -> {
          handle
              .createUpdate(
                  """
                  INSERT INTO estabelecimentos
                    (id, slug, nome, hora_abertura, hora_fechamento, abre_domingo,
                     max_veiculos_hora, ativo, criado_em, atualizado_em)
                  VALUES (:id, :slug, :name, :opening, :closing, :sunday, :capacity, TRUE,
                          :now, :now)
                  """)
              .bind("id", id)
              .bind("slug", slug)
              .bind("name", settings.name())
              .bind("opening", settings.openingHour())
              .bind("closing", settings.closingHour())
              .bind("sunday", settings.openOnSunday())
              .bind("capacity", settings.maxVehiclesPerHour())
              .bind("now", settings.configuredAt())
              .execute();
          handle
              .createUpdate(
                  """
                  INSERT INTO usuarios
                    (id, estabelecimento_id, nome, telefone, email, senha_hash, perfil,
                     criado_em, ativo)
                  VALUES (:id, :tenant, :name, :phone, :email, :hash, 'GERENTE', :createdAt, TRUE)
                  """)
              .bind("tenant", id)
              .bind("id", manager.id())
              .bind("name", manager.name())
              .bind("phone", manager.phone())
              .bind("email", manager.email())
              .bind("hash", managerPasswordHash)
              .bind("createdAt", manager.createdAt())
              .execute();
          seedCatalog(handle, id);
          return handle
              .createQuery(SUMMARY_SQL + " WHERE e.id = :id")
              .bind("id", id)
              .map((rs, ctx) -> summary(rs))
              .one();
        });
  }

  private static void seedCatalog(Handle handle, UUID establishmentId) {
    for (Object[] service : STARTER_SERVICES) {
      handle
          .createUpdate(
              "INSERT INTO servicos_preco (estabelecimento_id, nome, preco, comissao_lavador)"
                  + " VALUES (:tenant, :name, :price, :commission)")
          .bind("tenant", establishmentId)
          .bind("name", (String) service[0])
          .bind("price", new BigDecimal((String) service[1]))
          .bind("commission", new BigDecimal((String) service[2]))
          .execute();
    }
    for (String[] category : STARTER_EXPENSE_CATEGORIES) {
      handle
          .createUpdate(
              "INSERT INTO expense_category (id, estabelecimento_id, nome, cor, criado_em)"
                  + " VALUES (:id, :tenant, :name, :color, :now)")
          .bind("id", UUID.randomUUID())
          .bind("tenant", establishmentId)
          .bind("name", category[0])
          .bind("color", category[1])
          .bind("now", Instant.now())
          .execute();
    }
  }

  @Override
  public List<EstablishmentSummary> listEstablishments() {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SUMMARY_SQL + " ORDER BY e.nome")
                .map((rs, ctx) -> summary(rs))
                .list());
  }

  @Override
  public Optional<EstablishmentSummary> setEstablishmentActive(UUID id, boolean active) {
    return jdbi.inTransaction(
        handle -> {
          int updated =
              handle
                  .createUpdate(
                      "UPDATE estabelecimentos SET ativo = :active, atualizado_em = :now"
                          + " WHERE id = :id")
                  .bind("active", active)
                  .bind("now", Instant.now())
                  .bind("id", id)
                  .execute();
          if (updated != 1) {
            return Optional.empty();
          }
          return handle
              .createQuery(SUMMARY_SQL + " WHERE e.id = :id")
              .bind("id", id)
              .map((rs, ctx) -> summary(rs))
              .findOne();
        });
  }

  @Override
  public List<EstablishmentListing> searchActive(String query, int limit) {
    String pattern =
        "%"
            + query
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
            + "%";
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    """
                    SELECT slug, nome FROM estabelecimentos
                    WHERE ativo = TRUE
                      AND (LOWER(nome) LIKE :pattern ESCAPE '!' OR slug LIKE :pattern ESCAPE '!')
                    ORDER BY nome
                    LIMIT :limit
                    """)
                .bind("pattern", pattern)
                .bind("limit", limit)
                .map(
                    (rs, ctx) ->
                        new EstablishmentListing(rs.getString("slug"), rs.getString("nome")))
                .list());
  }

  private static EstablishmentSummary summary(ResultSet rs) throws SQLException {
    return new EstablishmentSummary(
        rs.getObject("id", UUID.class),
        rs.getString("slug"),
        rs.getString("nome"),
        rs.getBoolean("ativo"),
        rs.getTimestamp("criado_em").toInstant(),
        rs.getString("manager_name"),
        rs.getString("manager_email"));
  }
}
