package com.washflow.presentation.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
