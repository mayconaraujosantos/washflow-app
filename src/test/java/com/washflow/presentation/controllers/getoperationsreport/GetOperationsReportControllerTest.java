package com.washflow.presentation.controllers.getoperationsreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.usecases.GetOperationsReport;
import com.washflow.infra.db.jdbi.OperationsReport;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetOperationsReportControllerTest {

  @Mock private GetOperationsReport getOperationsReport;

  private GetOperationsReportController sut;

  @BeforeEach
  void setUp() {
    sut = new GetOperationsReportController(getOperationsReport);
  }

  @Test
  void getsReportAndReturnsOk() throws Exception {
    HttpRequest request =
        new HttpRequest(Map.of(), Map.of(), Map.of("from", "2026-01-01", "to", "2026-01-31"));

    OperationsReport report =
        new OperationsReport(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            10,
            new BigDecimal("600.00"),
            new BigDecimal("50.00"),
            45L,
            List.of(),
            List.of(),
            List.of());
    when(getOperationsReport.get(any(GetOperationsReport.Params.class))).thenReturn(report);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(getOperationsReport).get(any(GetOperationsReport.Params.class));
  }

  @Test
  void returnsBadRequestWhenDatesMissing() {
    HttpRequest request = new HttpRequest(Map.of(), Map.of(), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }
}
