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
  private static final String HTTP_DELETE = "DELETE";
  private static final String CASH_TRANSACTIONS = "/api/cash-transactions";

  private RoutePermissionMatcher() {}

  /**
   * Returns the required user profile for the given HTTP method and path, or {@code null} if no
   * specific role is required (e.g., public endpoints).
   */
  public static UserProfile requiredProfile(String method, String path) {
    if (path.startsWith("/api/admin/")) {
      return UserProfile.ADMIN;
    }
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
    return (HTTP_POST.equals(method) && path.equals("/api/vehicles"))
        || (HTTP_POST.equals(method) && path.equals("/api/service-orders"))
        || (HTTP_GET.equals(method)
            && path.matches("/api/customers/[^/]+/(vehicles|service-orders)"))
        || (HTTP_GET.equals(method) && path.matches("/api/service-orders/[^/]+"));
  }

  private static boolean isWasherResource(String method, String path) {
    return (HTTP_PATCH.equals(method) && path.matches("/api/service-orders/[^/]+/(start|complete)"))
        || (HTTP_PATCH.equals(method) && path.matches("/api/service-orders/[^/]+/(pause|resume)"))
        || (HTTP_GET.equals(method) && path.equals("/api/operations/queue"))
        || (HTTP_GET.equals(method) && path.equals("/api/operations/my-orders"));
  }

  private static boolean isManagerResource(String method, String path) {
    return switch (method) {
      case HTTP_GET ->
          path.matches("/api/operations/(report|yard|agenda|vehicles)")
              || path.equals("/api/users")
              || path.equals("/api/sales")
              || path.equals("/api/audit")
              || isFeatureResource(path)
              || path.equals(CASH_TRANSACTIONS + "/report");

      case HTTP_POST ->
          path.equals("/api/users")
              || path.equals("/api/operations/entries")
              || path.equals("/api/sales")
              || path.equals("/api/services")
              || path.matches("/api/users/[^/]+/badge")
              || path.matches("/api/service-orders/[^/]+/ticket")
              || isFeatureResource(path);

      case HTTP_PATCH ->
          path.matches("/api/users/[^/]+/(profile|active|email)")
              || path.matches("/api/service-orders/[^/]+/(check-in|finalize|cancel|refund)");

      case "PUT" -> path.matches(CASH_TRANSACTIONS + "/[^/]+") || path.equals("/api/establishment");

      case HTTP_DELETE ->
          path.matches(CASH_TRANSACTIONS + "/[^/]+")
              || path.matches("/api/service-orders/[^/]+/photos/[^/]+");

      default -> false;
    };
  }

  /**
   * Cash register, expense categories and the service order checklist/photo collections
   * (FeatureRoutes) are back-office tools; without an entry here the middleware denied them to
   * everyone.
   */
  private static boolean isFeatureResource(String path) {
    return path.equals(CASH_TRANSACTIONS)
        || path.equals("/api/expense-categories")
        || path.matches("/api/service-orders/[^/]+/(checklist|photos)");
  }
}
