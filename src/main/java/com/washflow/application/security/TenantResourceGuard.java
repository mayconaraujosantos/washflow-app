package com.washflow.application.security;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdbi.v3.core.Jdbi;

/**
 * Second line of defense for multi-lava-jato isolation: a request naming another establishment's
 * service order, account or cash entry by id is refused before any controller runs (repositories
 * also filter by establishment).
 */
final class TenantResourceGuard {

  private static final Pattern SERVICE_ORDER = Pattern.compile("^/api/service-orders/([^/]+)");
  private static final Pattern USER = Pattern.compile("^/api/(?:users|customers)/([^/]+)/");
  private static final Pattern CASH = Pattern.compile("^/api/cash-transactions/([^/]+)$");

  private TenantResourceGuard() {}

  static boolean allows(String path, UUID establishmentId, Jdbi jdbi) {
    if (establishmentId == null) {
      return true; // platform administrator: only /api/admin routes are reachable
    }
    String table = null;
    String id = null;
    for (Object[] rule :
        new Object[][] {
          {SERVICE_ORDER, "atendimentos"}, {USER, "usuarios"}, {CASH, "cash_transaction"}
        }) {
      Matcher matcher = ((Pattern) rule[0]).matcher(path);
      if (matcher.find()) {
        table = (String) rule[1];
        id = matcher.group(1);
        break;
      }
    }
    if (table == null || "report".equals(id)) {
      return true;
    }
    UUID resourceId;
    try {
      resourceId = UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      return true; // not an id: the controller answers 400/404 on its own
    }
    String sql = "SELECT estabelecimento_id FROM " + table + " WHERE id = :id";
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(sql)
                .bind("id", resourceId)
                .mapTo(UUID.class)
                .findOne()
                .map(establishmentId::equals)
                .orElse(true)); // unknown id: let the controller answer 404
  }
}
