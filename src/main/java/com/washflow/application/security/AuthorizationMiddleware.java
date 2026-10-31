package com.washflow.application.security;

import com.washflow.domain.entities.UserProfile;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jdbi.v3.core.Jdbi;

public final class AuthorizationMiddleware {

  private static final String CLAIMS_ATTRIBUTE = "washflow.auth.claims";
  private static final Set<String> PUBLIC_API_PATHS =
      Set.of("/api/health", "/api/hello", "/api/services");

  private final AuthTokenService tokenService;
  private final AccountSecurityGateway securityGateway;
  private final Jdbi jdbi;

  public AuthorizationMiddleware(
      AuthTokenService tokenService, AccountSecurityGateway securityGateway, Jdbi jdbi) {
    this.tokenService = tokenService;
    this.securityGateway = securityGateway;
    this.jdbi = jdbi;
  }

  public boolean authorize(Context ctx) {
    if (isPublicRoute(ctx)) {
      return true;
    }

    String token = extractToken(ctx);
    if (token == null) {
      return false;
    }

    AuthTokenService.Claims claims;
    try {
      claims = tokenService.verify(token);
    } catch (AuthTokenService.InvalidTokenException e) {
      ctx.status(401).json(Map.of("error", "Invalid or expired access token"));
      return false;
    }

    if (!validateAccountStatus(ctx, claims)) {
      return false;
    }

    if (!validateRoleAndResourcePermissions(ctx, claims)) {
      return false;
    }

    ctx.attribute(CLAIMS_ATTRIBUTE, claims);
    return true;
  }

  private boolean isPublicRoute(Context ctx) {
    String path = ctx.path();
    return !path.startsWith("/api/")
        || PUBLIC_API_PATHS.contains(path)
        || isAuthenticationPath(path, ctx.method().name());
  }

  private boolean isAuthenticationPath(String path, String method) {
    return method.equals("POST")
        && Set.of("/api/auth/customers", "/api/auth/washers", "/api/auth/managers").contains(path);
  }

  private String extractToken(Context ctx) {
    String authorization = ctx.header("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      ctx.status(401).json(Map.of("error", "Authentication required"));
      return null;
    }
    return authorization.substring("Bearer ".length());
  }

  private boolean validateAccountStatus(Context ctx, AuthTokenService.Claims claims) {
    Optional<AccountSecurityGateway.AccountAccess> account =
        securityGateway.findAccessById(claims.userId());

    if (account.isEmpty() || !account.get().active()) {
      ctx.status(401).json(Map.of("error", "Account is inactive"));
      return false;
    }

    if (!account.get().profile().equals(claims.profile())) {
      ctx.status(401).json(Map.of("error", "Access token is no longer valid"));
      return false;
    }
    return true;
  }

  private boolean validateRoleAndResourcePermissions(Context ctx, AuthTokenService.Claims claims) {
    UserProfile requiredProfile =
        RoutePermissionMatcher.requiredProfile(ctx.method().name(), ctx.path());
    if (requiredProfile == null || claims.profile() != requiredProfile) {
      ctx.status(403).json(Map.of("error", "Insufficient permissions"));
      return false;
    }

    Map<String, Object> body = parseBody(ctx);
    if (!ResourceAccessEvaluator.isAuthorized(
        ctx.method().name(), ctx.path(), body, claims.userId(), claims.profile(), jdbi)) {
      ctx.status(403).json(Map.of("error", "Resource is outside your account"));
      return false;
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseBody(Context ctx) {
    try {
      return ctx.bodyAsClass(Map.class);
    } catch (Exception e) {
      return Map.of();
    }
  }
}
