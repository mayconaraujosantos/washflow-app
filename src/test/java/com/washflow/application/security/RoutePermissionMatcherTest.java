package com.washflow.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.washflow.domain.entities.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RoutePermissionMatcherTest {

  @Test
  void returnsCustomerForCreateVehicle() {
    assertEquals(
        UserProfile.CUSTOMER, RoutePermissionMatcher.requiredProfile("POST", "/api/vehicles"));
  }

  @Test
  void returnsCustomerForCreateServiceOrder() {
    assertEquals(
        UserProfile.CUSTOMER,
        RoutePermissionMatcher.requiredProfile("POST", "/api/service-orders"));
  }

  @Test
  void returnsCustomerForListCustomerVehicles() {
    assertEquals(
        UserProfile.CUSTOMER,
        RoutePermissionMatcher.requiredProfile("GET", "/api/customers/abc-123/vehicles"));
  }

  @Test
  void returnsCustomerForListCustomerServiceOrders() {
    assertEquals(
        UserProfile.CUSTOMER,
        RoutePermissionMatcher.requiredProfile("GET", "/api/customers/abc-123/service-orders"));
  }

  @Test
  void returnsCustomerForGetServiceOrder() {
    assertEquals(
        UserProfile.CUSTOMER,
        RoutePermissionMatcher.requiredProfile("GET", "/api/service-orders/abc-123"));
  }

  @Test
  void returnsWasherForStartWash() {
    assertEquals(
        UserProfile.WASHER,
        RoutePermissionMatcher.requiredProfile("PATCH", "/api/service-orders/abc-123/start"));
  }

  @Test
  void returnsWasherForCompleteWash() {
    assertEquals(
        UserProfile.WASHER,
        RoutePermissionMatcher.requiredProfile("PATCH", "/api/service-orders/abc-123/complete"));
  }

  @Test
  void returnsWasherForPauseWash() {
    assertEquals(
        UserProfile.WASHER,
        RoutePermissionMatcher.requiredProfile("PATCH", "/api/service-orders/abc-123/pause"));
  }

  @Test
  void returnsWasherForResumeWash() {
    assertEquals(
        UserProfile.WASHER,
        RoutePermissionMatcher.requiredProfile("PATCH", "/api/service-orders/abc-123/resume"));
  }

  @Test
  void returnsWasherForWaitingQueue() {
    assertEquals(
        UserProfile.WASHER, RoutePermissionMatcher.requiredProfile("GET", "/api/operations/queue"));
  }

  @Test
  void returnsWasherForMyOrders() {
    assertEquals(
        UserProfile.WASHER,
        RoutePermissionMatcher.requiredProfile("GET", "/api/operations/my-orders"));
  }

  @Test
  void returnsManagerForOperationsReport() {
    assertEquals(
        UserProfile.MANAGER,
        RoutePermissionMatcher.requiredProfile("GET", "/api/operations/report"));
  }

  @Test
  void returnsManagerForListUsers() {
    assertEquals(UserProfile.MANAGER, RoutePermissionMatcher.requiredProfile("GET", "/api/users"));
  }

  @Test
  void returnsManagerForCreateWasher() {
    assertEquals(UserProfile.MANAGER, RoutePermissionMatcher.requiredProfile("POST", "/api/users"));
  }

  @Test
  void returnsManagerForUpdateUserProfile() {
    assertEquals(
        UserProfile.MANAGER,
        RoutePermissionMatcher.requiredProfile("PATCH", "/api/users/abc-123/profile"));
  }

  @Test
  void returnsManagerForUpdateUserActive() {
    assertEquals(
        UserProfile.MANAGER,
        RoutePermissionMatcher.requiredProfile("PATCH", "/api/users/abc-123/active"));
  }

  @Test
  void returnsManagerForCheckIn() {
    assertEquals(
        UserProfile.MANAGER,
        RoutePermissionMatcher.requiredProfile("PATCH", "/api/service-orders/abc-123/check-in"));
  }

  @Test
  void returnsManagerForFinalize() {
    assertEquals(
        UserProfile.MANAGER,
        RoutePermissionMatcher.requiredProfile("PATCH", "/api/service-orders/abc-123/finalize"));
  }

  @Test
  void returnsManagerForYardOverview() {
    assertEquals(
        UserProfile.MANAGER, RoutePermissionMatcher.requiredProfile("GET", "/api/operations/yard"));
  }

  @ParameterizedTest
  @CsvSource({
    "POST, /api/operations/entries",
    "GET, /api/operations/vehicles",
    "GET, /api/operations/agenda",
    "GET, /api/cash-transactions",
    "POST, /api/cash-transactions",
    "GET, /api/cash-transactions/report",
    "PUT, /api/cash-transactions/abc-123",
    "DELETE, /api/cash-transactions/abc-123",
    "GET, /api/expense-categories",
    "POST, /api/expense-categories",
    "GET, /api/service-orders/abc-123/checklist",
    "POST, /api/service-orders/abc-123/checklist",
    "GET, /api/service-orders/abc-123/photos",
    "POST, /api/service-orders/abc-123/photos",
    "DELETE, /api/service-orders/abc-123/photos/photo-1",
    "POST, /api/sales",
    "GET, /api/sales",
    "POST, /api/services",
    "PUT, /api/establishment",
    "PATCH, /api/users/abc-123/email",
    "POST, /api/users/abc-123/badge",
    "POST, /api/service-orders/abc-123/ticket"
  })
  void returnsManagerForBackOfficeRoutes(String method, String path) {
    assertEquals(UserProfile.MANAGER, RoutePermissionMatcher.requiredProfile(method, path));
  }

  @Test
  void keepsSingleServiceOrderLookupForCustomers() {
    assertEquals(
        UserProfile.CUSTOMER,
        RoutePermissionMatcher.requiredProfile("GET", "/api/service-orders/abc-123"));
  }

  @Test
  void returnsNullForUnknownRoute() {
    assertNull(RoutePermissionMatcher.requiredProfile("GET", "/api/unknown-endpoint"));
  }

  @Test
  void returnsNullForPublicHealthEndpoint() {
    assertNull(RoutePermissionMatcher.requiredProfile("GET", "/api/health"));
  }
}
