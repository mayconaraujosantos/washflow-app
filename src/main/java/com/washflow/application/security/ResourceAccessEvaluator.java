package com.washflow.application.security;

import com.washflow.domain.entities.user.UserProfile;
import java.util.Map;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

/**
 * Encapsulates the resource-level authorization logic that checks whether the authenticated user
 * has access to a specific resource (vehicle, service order, etc.).
 */
public final class ResourceAccessEvaluator {

  private ResourceAccessEvaluator() {}

  /**
   * Returns {@code true} if the user with the given ID and profile is authorized to access the
   * resource identified by the HTTP method, path, and body.
   *
   * @param method HTTP method (GET, POST, PATCH, etc.)
   * @param path request path
   * @param body parsed request body as a map, or empty map if no body
   * @param userId authenticated user's UUID
   * @param profile authenticated user's profile
   * @param jdbi Jdbi instance for database queries
   */
  public static boolean isAuthorized(
      String method,
      String path,
      Map<String, Object> body,
      UUID userId,
      UserProfile profile,
      Jdbi jdbi) {
    // Managers reach every resource of their own lava-jato (TenantResourceGuard checks which one);
    // the platform administrator only reaches /api/admin routes.
    if (profile == UserProfile.MANAGER || profile == UserProfile.ADMIN) {
      return true;
    }

    // A customer's own vehicles or service orders: the customer id in the path must be the
    // caller's.
    if (isCustomerResourcePath(path)) {
      return pathCustomerId(path).equals(userId.toString());
    }

    // Create vehicle: check customerId in body
    if (isCreateVehicle(method, path)) {
      return bodyValue(body, "customerId").equals(userId.toString());
    }

    // Create service order: check vehicle belongs to customer
    if (isCreateServiceOrder(method, path)) {
      return vehicleBelongsToCustomer(body, userId, jdbi);
    }

    // Start wash: check washerId in body matches user
    if (isStartWash(method, path)) {
      return bodyValue(body, "washerId").equals(userId.toString());
    }

    // Washer actions (complete, pause, resume): check service order assigned to washer
    if (isWasherAction(method, path)) {
      return hasAssignedServiceOrder(path, userId, jdbi);
    }

    // Update user profile: check managerId in body
    if (isUpdateUserProfile(method, path)) {
      return bodyValue(body, "managerId").equals(userId.toString());
    }

    // Get service order: check order belongs to customer's vehicle
    if (isGetServiceOrder(method, path)) {
      return serviceOrderBelongsToCustomer(path, userId, jdbi);
    }

    return true;
  }

  private static boolean isCustomerResourcePath(String path) {
    return path.matches("/api/customers/[^/]+/(vehicles|service-orders)");
  }

  private static String pathCustomerId(String path) {
    return path.split("/")[3];
  }

  private static boolean isCreateVehicle(String method, String path) {
    return method.equals("POST") && path.equals("/api/vehicles");
  }

  private static boolean isCreateServiceOrder(String method, String path) {
    return method.equals("POST") && path.equals("/api/service-orders");
  }

  private static boolean vehicleBelongsToCustomer(
      Map<String, Object> body, UUID userId, Jdbi jdbi) {
    String vehicleId = bodyValue(body, "vehicleId");
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

  private static boolean isStartWash(String method, String path) {
    return method.equals("PATCH") && path.matches("/api/service-orders/[^/]+/start");
  }

  private static boolean isWasherAction(String method, String path) {
    return method.equals("PATCH")
        && (path.matches("/api/service-orders/[^/]+/complete")
            || path.matches("/api/service-orders/[^/]+/(pause|resume)"));
  }

  private static boolean isUpdateUserProfile(String method, String path) {
    return method.equals("PATCH") && path.matches("/api/users/[^/]+/profile");
  }

  private static boolean isGetServiceOrder(String method, String path) {
    return method.equals("GET") && path.matches("/api/service-orders/[^/]+");
  }

  private static boolean serviceOrderBelongsToCustomer(String path, UUID userId, Jdbi jdbi) {
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

  private static boolean hasAssignedServiceOrder(String path, UUID userId, Jdbi jdbi) {
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
  private static String bodyValue(Map<String, Object> body, String key) {
    if (body == null || !body.containsKey(key)) {
      return "";
    }
    Object value = body.get(key);
    return value == null ? "" : value.toString();
  }
}
