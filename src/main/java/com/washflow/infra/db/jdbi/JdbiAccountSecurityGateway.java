package com.washflow.infra.db.jdbi;

import com.washflow.application.security.AccountSecurityGateway;
import com.washflow.domain.entities.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

/**
 * JDBI implementation of {@link AccountSecurityGateway} that queries the {@code usuarios} table.
 */
public class JdbiAccountSecurityGateway implements AccountSecurityGateway {

  private final Jdbi jdbi;

  public JdbiAccountSecurityGateway(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Optional<AccountAccess> findAccessById(UUID userId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT perfil, ativo FROM usuarios WHERE id = :id")
                .bind("id", userId)
                .map(
                    (rs, ctx) ->
                        new AccountAccess(
                            UserProfile.fromDbValue(rs.getString("perfil")),
                            rs.getBoolean("ativo")))
                .findOne());
  }
}
