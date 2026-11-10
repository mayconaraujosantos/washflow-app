package com.washflow.application.adapters;

import io.javalin.http.Context;

/**
 * Browser hardening headers for every response - API, PWA shell and static files.
 *
 * <p>The CSP allows only the app's own bundles plus Google Identity Services (the "Entrar com
 * Google" button). Photos and checklist signatures are shown as {@code data:} URLs. Inline styles
 * stay allowed (Google's button and React {@code style} props); inline scripts do not. Swagger UI
 * ships inline scripts, so {@code /swagger} and its {@code /webjars} assets get every header but
 * the CSP.
 */
public final class SecurityHeaders {

  public static final String CONTENT_SECURITY_POLICY =
      String.join(
          "; ",
          "default-src 'self'",
          "script-src 'self' https://accounts.google.com/gsi/client",
          "style-src 'self' 'unsafe-inline' https://accounts.google.com/gsi/style",
          "img-src 'self' data: blob:",
          "font-src 'self' data:",
          "connect-src 'self' https://accounts.google.com/gsi/",
          "frame-src https://accounts.google.com/gsi/",
          "worker-src 'self'",
          "manifest-src 'self'",
          "object-src 'none'",
          "base-uri 'self'",
          "form-action 'self'",
          "frame-ancestors 'none'");

  private SecurityHeaders() {}

  public static void apply(Context ctx) {
    ctx.header("X-Content-Type-Options", "nosniff");
    ctx.header("X-Frame-Options", "DENY");
    ctx.header("Referrer-Policy", "strict-origin-when-cross-origin");
    // Ignored by browsers over plain HTTP, so it is safe in local development too.
    ctx.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    // The washer login reads the badge QR with the camera.
    ctx.header("Permissions-Policy", "camera=(self), microphone=(), geolocation=()");
    if (!isSwaggerUi(ctx.path())) {
      ctx.header("Content-Security-Policy", CONTENT_SECURITY_POLICY);
    }
  }

  private static boolean isSwaggerUi(String path) {
    return path != null && (path.startsWith("/swagger") || path.startsWith("/webjars/"));
  }
}
