package com.washflow.presentation.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.washflow.domain.errors.common.ForbiddenError;
import com.washflow.domain.errors.common.TenantRequiredError;
import com.washflow.domain.errors.common.TicketNotFoundError;
import com.washflow.domain.errors.common.UnauthorizedError;
import com.washflow.domain.errors.serviceorder.InvalidPhotoTypeError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotCancellableError;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class HttpHelperTest {

  @AfterEach
  void clear() {
    MDC.clear();
  }

  @Test
  void errorBodiesCarryTheRequestIdAndRecordTheReason() {
    MDC.put(HttpHelper.REQUEST_ID, "abc123def456");

    var response = HttpHelper.badRequest(new IllegalArgumentException("Placa inválida: ZZ"));

    Map<?, ?> body = (Map<?, ?>) response.body();
    assertEquals("Placa inválida: ZZ", body.get("error"));
    assertEquals("abc123def456", body.get("requestId"));
    assertEquals("Placa inválida: ZZ", MDC.get(HttpHelper.ERROR_REASON));
  }

  @Test
  void worksOutsideARequest() {
    Map<?, ?> body = (Map<?, ?>) HttpHelper.notFound(new Exception("x")).body();

    assertFalse(body.containsKey("requestId"));
  }

  @Test
  void eachErrorCategoryHasItsOwnStatus() {
    assertEquals(400, HttpHelper.serverError(new InvalidPhotoTypeError("SIDE")).statusCode());
    assertEquals(
        401, HttpHelper.serverError(new UnauthorizedError("Sessão expirada")).statusCode());
    assertEquals(403, HttpHelper.serverError(new ForbiddenError("Só o gerente")).statusCode());
    assertEquals(404, HttpHelper.serverError(new TicketNotFoundError()).statusCode());
    assertEquals(409, HttpHelper.serverError(new ServiceOrderNotCancellableError()).statusCode());
  }

  @Test
  void aDomainErrorAnswersWithItsMessageCodeAndRequestId() {
    MDC.put(HttpHelper.REQUEST_ID, "abc123def456");

    HttpResponse response = HttpHelper.serverError(new TenantRequiredError());

    assertEquals(400, response.statusCode());
    assertEquals(
        Map.of(
            "error", "Escolha o lava-jato", "code", "TENANT_REQUIRED", "requestId", "abc123def456"),
        response.body());
  }

  @Test
  void anythingElseIsAnInternalErrorThatDoesNotLeakItsMessage() {
    HttpResponse response = HttpHelper.serverError(new IllegalStateException("pool exhausted"));

    assertEquals(500, response.statusCode());
    assertEquals(Map.of("error", "Internal server error"), response.body());
  }
}
