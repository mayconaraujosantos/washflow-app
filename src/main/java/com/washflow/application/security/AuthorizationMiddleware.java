package com.washflow.application.security;

import com.washflow.domain.entities.UserProfile;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

public final class AuthorizationMiddleware {

  private static final String CLAIMS_ATTRIBUTE = "washflow.auth.claims";
  private static final Set<String> PUBLIC_API_PATHS =
      Set.of("/api/health", "/api/hello", "/api/services");

  private final AuthTokenService tokenService;
  private final Jdbi jdbi;

  public AuthorizationMiddleware(AuthTokenService tokenService, Jdbi jdbi) {
    this.tokenService = tokenService;
    this.jdbi = jdbi;
  }

  public boolean authorize(Context ctx) {
    if (!ctx.path().startsWith("/api/")
        || PUBLIC_API_PATHS.contains(ctx.path())
        || isAuthenticationPath(ctx.path(), ctx.method().name())) {
      return true;
    }

    String authorization = ctx.header("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      ctx.status(401).json(Map.of("error", "Authentication required"));
      return false;
    }

    AuthTokenService.Claims claims;
    try {
      claims = tokenService.verify(authorization.substring("Bearer ".length()));
    } catch (AuthTokenService.InvalidTokenException e) {
      ctx.status(401).json(Map.of("error", "Invalid or expired access token"));
      return false;
    }
    AccountAccess account =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT perfil, ativo FROM usuarios WHERE id = :id")
                    .bind("id", claims.userId())
                    .map(
                        (rs, statementContext) ->
                            new AccountAccess(rs.getString("perfil"), rs.getBoolean("ativo")))
                    .findOne()
                    .orElse(null));
    if (account == null || !account.active()) {
      ctx.status(401).json(Map.of("error", "Account is inactive"));
      return false;
    }
    if (!UserProfile.fromDbValue(account.profile()).equals(claims.profile())) {
      ctx.status(401).json(Map.of("error", "Access token is no longer valid"));
      return false;
    }

    UserProfile requiredProfile = requiredProfile(ctx);
    if (requiredProfile == null || claims.profile() != requiredProfile) {
      ctx.status(403).json(Map.of("error", "Insufficient permissions"));
      return false;
    }
    if (!authorizedResource(ctx, claims.userId())) {
      ctx.status(403).json(Map.of("error", "Resource is outside your account"));
      return false;
    }
    ctx.attribute(CLAIMS_ATTRIBUTE, claims);
    return true;
  }

  private boolean isAuthenticationPath(String path, String method) {
    return method.equals("POST")
        && Set.of("/api/auth/customers", "/api/auth/washers", "/api/auth/managers").contains(path);
  }

  private UserProfile requiredProfile(Context ctx) {
    String path = ctx.path();
    String method = ctx.method().name();

    if (method.equals("POST") && path.equals("/api/vehicles")
        || method.equals("POST") && path.equals("/api/service-orders")
        || method.equals("GET") && path.matches("/api/customers/[^/]+/(vehicles|service-orders)")
        || method.equals("GET") && path.matches("/api/service-orders/[^/]+")) {
      return UserProfile.CUSTOMER;
    }
    if (method.equals("PATCH") && path.matches("/api/service-orders/[^/]+/(start|complete)")) {
      return UserProfile.WASHER;
    }
    if (method.equals("PATCH") && path.matches("/api/service-orders/[^/]+/(pause|resume)")) {
      return UserProfile.WASHER;
    }
    if (method.equals("GET") && path.equals("/api/operations/queue")) {
      return UserProfile.WASHER;
    }
    if (method.equals("GET") && path.equals("/api/operations/my-orders")) {
      return UserProfile.WASHER;
    }
    if (method.equals("GET") && path.equals("/api/operations/report")) {
      return UserProfile.MANAGER;
    }
    if (path.equals("/api/users") && Set.of("GET", "POST").contains(method)
        || method.equals("PATCH") && path.matches("/api/users/[^/]+/(profile|active)")) {
      return UserProfile.MANAGER;
    }
    if (method.equals("PATCH") && path.matches("/api/service-orders/[^/]+/(check-in|finalize)")
        || method.equals("GET") && path.equals("/api/operations/yard")) {
      return UserProfile.MANAGER;
    }
    return null;
  }

  private boolean authorizedResource(Context ctx, UUID userId) {
    String path = ctx.path();
    String method = ctx.method().name();

    if (path.matches("/api/customers/[^/]+/(vehicles|service-orders)")) {
      return path.split("/")[3].equals(userId.toString());
    }
    if (method.equals("POST") && path.equals("/api/vehicles")) {
      return bodyValue(ctx, "customerId").equals(userId.toString());
    }
    if (method.equals("POST") && path.equals("/api/service-orders")) {
      String vehicleId = bodyValue(ctx, "vehicleId");
      try {
        return jdbi.withHandle(
            handle ->
                handle
                        .createQuery(
                            "SELECT COUNT(*) FROM veiculos WHERE id = :vehicleId AND cliente_id = :customerId")
                        .bind("vehicleId", UUID.fromString(vehicleId))
                        .bind("customerId", userId)
                        .mapTo(Integer.class)
                        .one()
                    == 1);
      } catch (IllegalArgumentException e) {
        return false;
      }
    }
    if (method.equals("PATCH") && path.matches("/api/service-orders/[^/]+/start")) {
      return bodyValue(ctx, "washerId").equals(userId.toString());
    }
    if (method.equals("PATCH") && path.matches("/api/service-orders/[^/]+/complete")) {
      return hasAssignedServiceOrder(path, userId);
    }
    if (method.equals("PATCH") && path.matches("/api/service-orders/[^/]+/(pause|resume)")) {
      return hasAssignedServiceOrder(path, userId);
    }
    if (method.equals("PATCH") && path.matches("/api/users/[^/]+/profile")) {
      return bodyValue(ctx, "managerId").equals(userId.toString());
    }
    if (method.equals("GET") && path.matches("/api/service-orders/[^/]+")) {
      try {
        UUID orderId = UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
        return jdbi.withHandle(
            handle ->
                handle
                        .createQuery(
                            """
                        SELECT COUNT(*) FROM atendimentos a
                        JOIN veiculos v ON v.id = a.veiculo_id
                        WHERE a.id = :orderId AND v.cliente_id = :customerId
                        """)
                        .bind("orderId", orderId)
                        .bind("customerId", userId)
                        .mapTo(Integer.class)
                        .one()
                    == 1);
      } catch (IllegalArgumentException e) {
        return false;
      }
    }
    return true;
  }

  private boolean hasAssignedServiceOrder(String path, UUID userId) {
    try {
      UUID orderId = UUID.fromString(path.split("/")[3]);
      return jdbi.withHandle(
          handle ->
              handle
                      .createQuery(
                          "SELECT COUNT(*) FROM atendimentos WHERE id = :orderId AND lavador_id = :washerId")
                      .bind("orderId", orderId)
                      .bind("washerId", userId)
                      .mapTo(Integer.class)
                      .one()
                  == 1);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  private String bodyValue(Context ctx, String key) {
    try {
      Object value = ctx.bodyAsClass(Map.class).get(key);
      return value == null ? "" : value.toString();
    } catch (Exception e) {
      return "";
    }
  }

  private record AccountAccess(String profile, boolean active) {}
}
