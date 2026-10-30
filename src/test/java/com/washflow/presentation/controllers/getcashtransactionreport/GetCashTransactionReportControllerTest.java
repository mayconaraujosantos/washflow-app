package com.washflow.presentation.controllers.getcashtransactionreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.GetCashTransactionReport;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCashTransactionReportControllerTest {

  @Mock private GetCashTransactionReport getCashTransactionReport;

  private GetCashTransactionReportController sut;

  @BeforeEach
  void setUp() {
    sut = new GetCashTransactionReportController(getCashTransactionReport);
  }

  @Test
  void returnsReportWhenValidDates() throws Exception {
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate to = LocalDate.of(2025, 1, 31);
    Map<String, Object> expected = new HashMap<>();
    expected.put("totalIncome", 1000.0);
    when(getCashTransactionReport.report(eq(from), eq(to))).thenReturn(expected);

    Map<String, Object> body = new HashMap<>();
    body.put("from", "2025-01-01");
    body.put("to", "2025-01-31");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(getCashTransactionReport).report(from, to);
  }

  @Test
  void returnsBadRequestWhenFromIsMissing() {
    Map<String, Object> body = new HashMap<>();
    body.put("to", "2025-01-31");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenToIsMissing() {
    Map<String, Object> body = new HashMap<>();
    body.put("from", "2025-01-01");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenToIsBeforeFrom() {
    Map<String, Object> body = new HashMap<>();
    body.put("from", "2025-02-01");
    body.put("to", "2025-01-01");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenRangeExceeds365Days() {
    Map<String, Object> body = new HashMap<>();
    body.put("from", "2024-01-01");
    body.put("to", "2025-12-31");
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
