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

  private static final String CUSTOMER_ID = "customerId";
  private static final String HTTP_PATCH = "PATCH";

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
    if (profile == UserProfile.MANAGER || profile == UserProfile.ADMIN) {
      return true;
    }

    if (isCustomerResourcePath(path)) {
      return pathCustomerId(path).equals(userId.toString());
    }

    if (isCreateVehicle(method, path)) {
      return bodyValue(body, CUSTOMER_ID).equals(userId.toString());
    }

    if (isCreateServiceOrder(method, path)) {
      return vehicleBelongsToCustomer(body, userId, jdbi);
    }

    if (isStartWash(method, path)) {
      return bodyValue(body, "washerId").equals(userId.toString());
    }

    if (isWasherAction(method, path)) {
      return hasAssignedServiceOrder(path, userId, jdbi);
    }

    if (isUpdateUserProfile(method, path)) {
      return bodyValue(body, "managerId").equals(userId.toString());
    }

    if (isGetServiceOrder(method, path)) {
      return serviceOrderBelongsToCustomer(path, userId, jdbi);
    }

    return true;
  }

  private static boolean isCustomerResourcePath(String path) {
    return path.matches(
        "/api/customers/[^/]+/(vehicles|unclaimed-vehicles|service-orders|recovery-email)");
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
                      .bind(CUSTOMER_ID, userId)
                      .mapTo(Integer.class)
                      .one()
                  == 1);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean isStartWash(String method, String path) {
    return method.equals(HTTP_PATCH) && path.matches("/api/service-orders/[^/]+/start");
  }

  private static boolean isWasherAction(String method, String path) {
    return method.equals(HTTP_PATCH)
        && (path.matches("/api/service-orders/[^/]+/complete")
            || path.matches("/api/service-orders/[^/]+/(pause|resume)"));
  }

  private static boolean isUpdateUserProfile(String method, String path) {
    return method.equals(HTTP_PATCH) && path.matches("/api/users/[^/]+/profile");
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
                      .bind(CUSTOMER_ID, userId)
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

  private static String bodyValue(Map<String, Object> body, String key) {
    if (body == null || !body.containsKey(key)) {
      return "";
    }
    Object value = body.get(key);
    return value == null ? "" : value.toString();
  }
}
