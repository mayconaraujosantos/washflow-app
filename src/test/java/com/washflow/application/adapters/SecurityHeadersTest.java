package com.washflow.application.adapters;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class SecurityHeadersTest {

  @Test
  void everyResponseCarriesTheBrowserHardeningHeaders() {
    Context ctx = request("/gerente");

    SecurityHeaders.apply(ctx);

    verify(ctx).header("X-Content-Type-Options", "nosniff");
    verify(ctx).header("X-Frame-Options", "DENY");
    verify(ctx).header("Referrer-Policy", "strict-origin-when-cross-origin");
    verify(ctx).header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    verify(ctx).header("Permissions-Policy", "camera=(self), microphone=(), geolocation=()");
    verify(ctx)
        .header(ArgumentMatchers.eq("Content-Security-Policy"), ArgumentMatchers.anyString());
  }

  @Test
  void theCspOnlyAllowsTheAppItselfAndGoogleSignIn() {
    String csp = SecurityHeaders.CONTENT_SECURITY_POLICY;

    assertTrue(csp.contains("default-src 'self'"), csp);
    assertTrue(csp.contains("script-src 'self' https://accounts.google.com/gsi/client"), csp);
    assertTrue(csp.contains("frame-src https://accounts.google.com/gsi/"), csp);
    assertTrue(csp.contains("img-src 'self' data: blob:"), "photos and signatures are data URLs");
    assertTrue(csp.contains("frame-ancestors 'none'"), csp);
    assertTrue(csp.contains("object-src 'none'"), csp);
    assertTrue(!csp.contains("script-src 'self' 'unsafe-inline'"), "no inline scripts");
  }

  @Test
  void swaggerUiKeepsItsInlineScriptsSoItHasNoCsp() {
    for (String path : new String[] {"/swagger", "/webjars/swagger-ui/5.17.14/index.css"}) {
      Context ctx = request(path);

      SecurityHeaders.apply(ctx);

      verify(ctx).header("X-Content-Type-Options", "nosniff");
      verify(ctx, never())
          .header(ArgumentMatchers.eq("Content-Security-Policy"), ArgumentMatchers.anyString());
    }
  }

  private static Context request(String path) {
    Context ctx = mock(Context.class, RETURNS_SELF);
    when(ctx.path()).thenReturn(path);
    return ctx;
  }
}
