package com.washflow.infra.db.jdbi;

import com.washflow.application.security.AccountSecurityGateway;
import com.washflow.domain.entities.user.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

/**
 * JDBI implementation of {@link AccountSecurityGateway}: establishment accounts in {@code usuarios}
 * (inactive when either the account or its lava-jato is suspended) and the platform administrators
 * in {@code administradores}.
 */
public class JdbiAccountSecurityGateway implements AccountSecurityGateway {

  private final Jdbi jdbi;

  public JdbiAccountSecurityGateway(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Optional<AccountAccess> findAccessById(UUID userId) {
    Optional<AccountAccess> member =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        """
                        SELECT u.perfil, u.ativo, e.ativo AS estabelecimento_ativo,
                               u.estabelecimento_id
                        FROM usuarios u
                        JOIN estabelecimentos e ON e.id = u.estabelecimento_id
                        WHERE u.id = :id
                        """)
                    .bind("id", userId)
                    .map(
                        (rs, ctx) ->
                            new AccountAccess(
                                UserProfile.fromDbValue(rs.getString("perfil")),
                                rs.getBoolean("ativo") && rs.getBoolean("estabelecimento_ativo"),
                                rs.getObject("estabelecimento_id", UUID.class)))
                    .findOne());
    if (member.isPresent()) {
      return member;
    }
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT id FROM administradores WHERE id = :id")
                .bind("id", userId)
                .mapTo(UUID.class)
                .findOne()
                .map(id -> new AccountAccess(UserProfile.ADMIN, true, null)));
  }

  @Override
  public Optional<UUID> findActiveEstablishmentBySlug(String slug) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT id FROM estabelecimentos WHERE slug = :slug AND ativo = TRUE")
                .bind("slug", slug)
                .mapTo(UUID.class)
                .findOne());
  }
}
