package com.washflow.presentation.controllers.user;

import static com.washflow.presentation.helpers.HttpRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.WasherBadge;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IssueWasherBadgeControllerTest {

  @Test
  void issuesTheBadgeOfTheWasherInThePath() throws Exception {
    WasherBadge badge = mock(WasherBadge.class);
    UUID id = UUID.randomUUID();
    WasherBadge.Issued issued = new WasherBadge.Issued("123456", "token");
    when(badge.issue(id)).thenReturn(issued);

    HttpResponse response =
        new IssueWasherBadgeController(badge).handle(request(Map.of("id", id.toString())));

    assertEquals(200, response.statusCode());
    assertEquals(issued, response.body());
  }

  @Test
  void rejectsAnInvalidIdAndAnUnknownWasher() throws Exception {
    WasherBadge badge = mock(WasherBadge.class);
    when(badge.issue(any())).thenThrow(new UserNotFoundError(UUID.randomUUID()));
    IssueWasherBadgeController sut = new IssueWasherBadgeController(badge);

    assertEquals(400, sut.handle(request(Map.of("id", "x"))).statusCode());
    assertEquals(404, sut.handle(request(Map.of("id", UUID.randomUUID().toString()))).statusCode());
  }
}
