package com.washflow.application.security;

import com.washflow.data.tenancy.TenantContext;
import com.washflow.domain.entities.UserProfile;
import com.washflow.presentation.helpers.HttpHelper;
import io.javalin.http.Context;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

public final class AuthorizationMiddleware {

  private static final String CLAIMS_ATTRIBUTE = "washflow.auth.claims";
  private static final Set<String> PUBLIC_API_PATHS =
      Set.of(
          "/api/health",
          "/api/hello",
          "/api/services",
          "/api/setup",
          "/api/establishment",
          "/api/auth/options",
          "/api/schedule/availability",
          "/api/metrics",
          "/api/establishments");

  // Public screens that only make sense inside one lava-jato: the customer must have picked it
  // (X-Establishment header) or be signed in to it.
  private static final Set<String> TENANT_REQUIRED_PATHS =
      Set.of(
          "/api/services",
          "/api/establishment",
          "/api/schedule/availability",
          "/api/auth/customers",
          "/api/auth/customers/otp",
          "/api/auth/customers/otp/verify",
          "/api/auth/customers/google");

  public static final String ESTABLISHMENT_HEADER = "X-Establishment";
  // Login entry points (POST): each one fixes the profile it can sign into.
  private static final Set<String> AUTHENTICATION_PATHS =
      Set.of(
          "/api/auth/customers",
          "/api/auth/washers",
          "/api/auth/managers",
          "/api/setup",
          "/api/auth/washers/pin",
          "/api/auth/washers/badge",
          "/api/auth/customers/otp",
          "/api/auth/customers/otp/verify",
          "/api/auth/customers/google",
          "/api/auth/admins",
          "/api/client-errors");
  public static final String ERROR = "error";

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
    TenantContext.clear();
    if (isPublicRoute(ctx)) {
      return resolvePublicTenant(ctx);
    }

    String token = extractToken(ctx);
    if (token == null) {
      return false;
    }

    AuthTokenService.Claims claims;
    try {
      claims = tokenService.verify(token);
    } catch (AuthTokenService.InvalidTokenException e) {
      ctx.status(401).json(HttpHelper.errorBody("Invalid or expired access token"));
      return false;
    }

    Optional<AccountSecurityGateway.AccountAccess> access = validateAccountStatus(ctx, claims);
    if (access.isEmpty()) {
      return false;
    }
    UUID establishmentId = access.get().establishmentId();
    if (establishmentId != null) {
      TenantContext.set(establishmentId);
    }

    if (!validateRoleAndResourcePermissions(ctx, claims)) {
      return false;
    }

    if (!TenantResourceGuard.allows(ctx.path(), establishmentId, jdbi)) {
      ctx.status(403).json(HttpHelper.errorBody("Resource is outside your account"));
      return false;
    }

    ctx.attribute(CLAIMS_ATTRIBUTE, claims);
    return true;
  }

  private boolean isPublicRoute(Context ctx) {
    String path = ctx.path();
    // Public API paths are read-only: POST /api/services (catalog) must still be authorized.
    return !path.startsWith("/api/")
        || (PUBLIC_API_PATHS.contains(path) && "GET".equals(ctx.method().name()))
        // Ticket links are unguessable tokens meant to be opened without logging in.
        || ("GET".equals(ctx.method().name()) && path.matches("/api/tickets/[^/]+"))
        || isAuthenticationPath(path, ctx.method().name());
  }

  private boolean isAuthenticationPath(String path, String method) {
    return method.equals("POST")
        // POST /api/setup is open only while the establishment is unconfigured (and, in
        // production, with the server's SETUP_CODE) - DbSetupEstablishment enforces both.
        && AUTHENTICATION_PATHS.contains(path);
  }

  private String extractToken(Context ctx) {
    String authorization = ctx.header("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      ctx.status(401).json(HttpHelper.errorBody("Authentication required"));
      return null;
    }
    return authorization.substring("Bearer ".length());
  }

  private Optional<AccountSecurityGateway.AccountAccess> validateAccountStatus(
      Context ctx, AuthTokenService.Claims claims) {
    Optional<AccountSecurityGateway.AccountAccess> account =
        securityGateway.findAccessById(claims.userId());

    if (account.isEmpty() || !account.get().active()) {
      ctx.status(401).json(HttpHelper.errorBody("Account is inactive"));
      return Optional.empty();
    }

    if (!account.get().profile().equals(claims.profile())) {
      ctx.status(401).json(HttpHelper.errorBody("Access token is no longer valid"));
      return Optional.empty();
    }
    return account;
  }

  /**
   * Public routes still run inside a lava-jato when one is known: the signed-in account's (staff
   * screens read the public catalog too) or the one picked on the home screen.
   */
  private boolean resolvePublicTenant(Context ctx) {
    Optional<UUID> fromAccount = establishmentOfOptionalToken(ctx);
    if (fromAccount.isPresent()) {
      TenantContext.set(fromAccount.get());
      return true;
    }
    // Only screens that work inside a lava-jato read the picked one: the directory, tickets and
    // staff logins must keep working even if the remembered lava-jato was suspended.
    if (!TENANT_REQUIRED_PATHS.contains(ctx.path())) {
      return true;
    }
    String slug = ctx.header(ESTABLISHMENT_HEADER);
    if (slug != null && !slug.isBlank()) {
      Optional<UUID> picked = securityGateway.findActiveEstablishmentBySlug(slug.trim());
      if (picked.isEmpty()) {
        ctx.status(404).json(HttpHelper.errorBody("Lava-jato não encontrado"));
        return false;
      }
      TenantContext.set(picked.get());
      return true;
    }
    ctx.status(400).json(HttpHelper.errorBody("Escolha o lava-jato"));
    return false;
  }

  private Optional<UUID> establishmentOfOptionalToken(Context ctx) {
    String authorization = ctx.header("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return Optional.empty();
    }
    try {
      AuthTokenService.Claims claims =
          tokenService.verify(authorization.substring("Bearer ".length()));
      return securityGateway
          .findAccessById(claims.userId())
          .filter(AccountSecurityGateway.AccountAccess::active)
          .map(AccountSecurityGateway.AccountAccess::establishmentId);
    } catch (AuthTokenService.InvalidTokenException e) {
      return Optional.empty();
    }
  }

  private boolean validateRoleAndResourcePermissions(Context ctx, AuthTokenService.Claims claims) {
    UserProfile requiredProfile =
        RoutePermissionMatcher.requiredProfile(ctx.method().name(), ctx.path());
    if (requiredProfile == null || claims.profile() != requiredProfile) {
      ctx.status(403).json(HttpHelper.errorBody("Insufficient permissions"));
      return false;
    }

    Map<String, Object> body = parseBody(ctx);
    if (!ResourceAccessEvaluator.isAuthorized(
        ctx.method().name(), ctx.path(), body, claims.userId(), claims.profile(), jdbi)) {
      ctx.status(403).json(HttpHelper.errorBody("Resource is outside your account"));
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
