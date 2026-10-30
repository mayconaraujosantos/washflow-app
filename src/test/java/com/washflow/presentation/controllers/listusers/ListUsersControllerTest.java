package com.washflow.presentation.controllers.listusers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.usecases.ListUsers;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListUsersControllerTest {

  @Mock private ListUsers listUsers;

  private ListUsersController sut;

  @BeforeEach
  void setUp() {
    sut = new ListUsersController(listUsers);
  }

  @Test
  void listsUsersAndReturnsOk() throws Exception {
    List<User> users =
        List.of(
            new User(
                UUID.randomUUID(),
                "User 1",
                "11999999999",
                UserProfile.CUSTOMER,
                Instant.now(),
                true),
            new User(
                UUID.randomUUID(),
                "User 2",
                "11999999998",
                UserProfile.WASHER,
                Instant.now(),
                true));
    when(listUsers.list()).thenReturn(users);

    HttpRequest request = new HttpRequest(Map.of(), Map.of(), Map.of());
    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(listUsers).list();
  }

  @Test
  void returnsEmptyListWhenNoUsers() throws Exception {
    when(listUsers.list()).thenReturn(List.of());

    HttpRequest request = new HttpRequest(Map.of(), Map.of(), Map.of());
    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
  }
}
