package com.washflow.data.tenancy;

import com.washflow.domain.errors.common.TenantRequiredError;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The establishment (lava-jato) the current request works for.
 *
 * <p>Set once per request by the authorization middleware - from the signed-in account, or from the
 * {@code X-Establishment} header on public screens - and cleared when the request ends. Every
 * repository query that says {@code estabelecimento_id = :tenant} gets this value bound
 * automatically (see {@code TenantBindingCustomizer}), so data of another establishment can't be
 * read or written by forgetting a filter argument: without a tenant the query fails instead.
 */
public final class TenantContext {

  private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

  private TenantContext() {}

  public static void set(UUID establishmentId) {
    CURRENT.set(establishmentId);
  }

  public static Optional<UUID> current() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static UUID require() {
    UUID id = CURRENT.get();
    if (id == null) {
      throw new TenantRequiredError();
    }
    return id;
  }

  public static void clear() {
    CURRENT.remove();
  }

  /** Runs {@code work} as establishment {@code establishmentId}, restoring the previous one. */
  public static <T> T runAs(UUID establishmentId, Supplier<T> work) {
    UUID previous = CURRENT.get();
    CURRENT.set(establishmentId);
    try {
      return work.get();
    } finally {
      if (previous == null) {
        CURRENT.remove();
      } else {
        CURRENT.set(previous);
      }
    }
  }
}
