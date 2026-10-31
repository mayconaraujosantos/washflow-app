package com.washflow.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.washflow.domain.entities.UserProfile;
import org.junit.jupiter.api.Test;

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

  @Test
  void returnsNullForUnknownRoute() {
    assertNull(RoutePermissionMatcher.requiredProfile("GET", "/api/unknown-endpoint"));
  }

  @Test
  void returnsNullForPublicHealthEndpoint() {
    assertNull(RoutePermissionMatcher.requiredProfile("GET", "/api/health"));
  }
}
