package com.washflow.application.security;

import com.washflow.domain.entities.UserProfile;

/**
 * Encapsulates the route-to-profile mapping logic that determines which user role is required to
 * access a given HTTP endpoint.
 */
public final class RoutePermissionMatcher {
  private static final String HTTP_GET = "GET";
  private static final String HTTP_POST = "POST";
  private static final String HTTP_PATCH = "PATCH";

  private RoutePermissionMatcher() {}

  /**
   * Returns the required user profile for the given HTTP method and path, or {@code null} if no
   * specific role is required (e.g., public endpoints).
   */
  public static UserProfile requiredProfile(String method, String path) {
    if (isCustomerResource(method, path)) {
      return UserProfile.CUSTOMER;
    }
    if (isWasherResource(method, path)) {
      return UserProfile.WASHER;
    }
    if (isManagerResource(method, path)) {
      return UserProfile.MANAGER;
    }
    return null;
  }

  private static boolean isCustomerResource(String method, String path) {
    return (method.equals("POST") && path.equals("/api/vehicles"))
        || (method.equals("POST") && path.equals("/api/service-orders"))
        || (method.equals("GET") && path.matches("/api/customers/[^/]+/(vehicles|service-orders)"))
        || (method.equals("GET") && path.matches("/api/service-orders/[^/]+"));
  }

  private static boolean isWasherResource(String method, String path) {
    return (method.equals("PATCH") && path.matches("/api/service-orders/[^/]+/(start|complete)"))
        || (method.equals("PATCH") && path.matches("/api/service-orders/[^/]+/(pause|resume)"))
        || (method.equals("GET") && path.equals("/api/operations/queue"))
        || (method.equals("GET") && path.equals("/api/operations/my-orders"));
  }

  private static boolean isManagerResource(String method, String path) {
    return switch (method) {
      case "GET" ->
          path.equals("/api/operations/report")
              || path.equals("/api/operations/yard")
              || path.equals("/api/users");

      case "POST" -> path.equals("/api/users");

      case "PATCH" ->
          path.matches("/api/users/[^/]+/(profile|active)")
              || path.matches("/api/service-orders/[^/]+/(check-in|finalize)");

      default -> false;
    };
  }
}
