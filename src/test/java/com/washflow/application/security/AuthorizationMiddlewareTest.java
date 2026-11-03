package com.washflow.application.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.UserProfile;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthorizationMiddlewareTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";
  private static final UUID TEST_USER_ID = UUID.randomUUID();

  private final AuthTokenService tokenService = new AuthTokenService(SECRET);
  private final AccountSecurityGateway securityGateway = mock(AccountSecurityGateway.class);
  private final Jdbi jdbi = mock(Jdbi.class);

  private AuthorizationMiddleware createMiddleware() {
    return new AuthorizationMiddleware(tokenService, securityGateway, jdbi);
  }

  @Test
  void allowsPublicHealthEndpoint() {
    Context ctx = mockContext("GET", "/api/health", null, null);
    AuthorizationMiddleware middleware = createMiddleware();

    assertTrue(middleware.authorize(ctx));
  }

  @Test
  void allowsStaticFiles() {
    Context ctx = mockContext("GET", "/index.html", null, null);
    AuthorizationMiddleware middleware = createMiddleware();

    assertTrue(middleware.authorize(ctx));
  }

  @Test
  void allowsAuthenticationEndpoints() {
    Context ctx = mockContext("POST", "/api/auth/customers", null, null);
    AuthorizationMiddleware middleware = createMiddleware();

    assertTrue(middleware.authorize(ctx));
  }

  @Test
  void rejectsMissingToken() {
    Context ctx = mockContext("GET", "/api/vehicles", null, null);
    AuthorizationMiddleware middleware = createMiddleware();

    assertFalse(middleware.authorize(ctx));
  }

  @Test
  void rejectsInvalidToken() {
    Context ctx = mockContext("GET", "/api/vehicles", "Bearer invalid-token", null);
    AuthorizationMiddleware middleware = createMiddleware();

    assertFalse(middleware.authorize(ctx));
  }

  @Test
  void rejectsInactiveAccount() {
    String token = createTokenForUser(TEST_USER_ID, UserProfile.CUSTOMER);
    when(securityGateway.findAccessById(TEST_USER_ID))
        .thenReturn(
            Optional.of(new AccountSecurityGateway.AccountAccess(UserProfile.CUSTOMER, false)));

    Context ctx = mockContext("GET", "/api/vehicles", "Bearer " + token, null);
    AuthorizationMiddleware middleware = createMiddleware();

    assertFalse(middleware.authorize(ctx));
  }

  @Test
  void rejectsNonExistentAccount() {
    String token = createTokenForUser(TEST_USER_ID, UserProfile.CUSTOMER);
    when(securityGateway.findAccessById(TEST_USER_ID)).thenReturn(Optional.empty());

    Context ctx = mockContext("GET", "/api/vehicles", "Bearer " + token, null);
    AuthorizationMiddleware middleware = createMiddleware();

    assertFalse(middleware.authorize(ctx));
  }

  @Test
  void rejectsProfileMismatch() {
    String token = createTokenForUser(TEST_USER_ID, UserProfile.CUSTOMER);
    when(securityGateway.findAccessById(TEST_USER_ID))
        .thenReturn(
            Optional.of(new AccountSecurityGateway.AccountAccess(UserProfile.MANAGER, true)));

    Context ctx = mockContext("GET", "/api/vehicles", "Bearer " + token, null);
    AuthorizationMiddleware middleware = createMiddleware();

    assertFalse(middleware.authorize(ctx));
  }

  @Test
  void rejectsInsufficientPermissions() {
    String token = createTokenForUser(TEST_USER_ID, UserProfile.CUSTOMER);
    when(securityGateway.findAccessById(TEST_USER_ID))
        .thenReturn(
            Optional.of(new AccountSecurityGateway.AccountAccess(UserProfile.CUSTOMER, true)));

    Context ctx = mockContext("GET", "/api/users", "Bearer " + token, null);
    AuthorizationMiddleware middleware = createMiddleware();

    assertFalse(middleware.authorize(ctx));
  }

  @Test
  void allowsAuthorizedRequest() {
    String token = createTokenForUser(TEST_USER_ID, UserProfile.CUSTOMER);
    when(securityGateway.findAccessById(TEST_USER_ID))
        .thenReturn(
            Optional.of(new AccountSecurityGateway.AccountAccess(UserProfile.CUSTOMER, true)));

    Map<String, Object> body = Map.of("customerId", TEST_USER_ID.toString());
    Context ctx = mockContext("POST", "/api/vehicles", "Bearer " + token, body);
    AuthorizationMiddleware middleware = createMiddleware();

    assertTrue(middleware.authorize(ctx));
  }

  @Test
  void allowsManagerToAccessAllResources() {
    String token = createTokenForUser(TEST_USER_ID, UserProfile.MANAGER);
    when(securityGateway.findAccessById(TEST_USER_ID))
        .thenReturn(
            Optional.of(new AccountSecurityGateway.AccountAccess(UserProfile.MANAGER, true)));

    Context ctx = mockContext("GET", "/api/users", "Bearer " + token, Map.of());
    AuthorizationMiddleware middleware = createMiddleware();

    assertTrue(middleware.authorize(ctx));
  }

  private String createTokenForUser(UUID userId, UserProfile profile) {
    com.washflow.domain.entities.User user =
        new com.washflow.domain.entities.User(
            userId, "Test", "11999990000", profile, java.time.Instant.now());
    return tokenService.issue(user);
  }

  private Context mockContext(
      String method, String path, String authorization, Map<String, Object> body) {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.findOrCreate(method));
    when(ctx.path()).thenReturn(path);
    when(ctx.header("Authorization")).thenReturn(authorization);
    when(ctx.status(any(Integer.class))).thenReturn(ctx);
    when(ctx.json(any())).thenReturn(ctx);
    when(ctx.bodyAsClass(Map.class)).thenReturn(body != null ? body : Map.of());
    return ctx;
  }
}
