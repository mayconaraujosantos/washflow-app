package com.washflow.data.usecases.getoperationsreport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadOperationsReportRepository;
import com.washflow.domain.usecases.GetOperationsReport;
import com.washflow.infra.db.jdbi.OperationsReport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbGetOperationsReportTest {

  @Mock private LoadOperationsReportRepository loadRepository;

  private DbGetOperationsReport sut;
  private LocalDate from;
  private LocalDate to;

  @BeforeEach
  void setUp() {
    sut = new DbGetOperationsReport(loadRepository);
    from = LocalDate.of(2025, 1, 1);
    to = LocalDate.of(2025, 1, 31);
  }

  @Test
  void returnsReportWhenRangeIsValid() {
    OperationsReport expected =
        new OperationsReport(
            from,
            to,
            10,
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(100),
            30L,
            List.of(),
            List.of(),
            List.of());
    when(loadRepository.load(any(), any())).thenReturn(expected);

    GetOperationsReport.Params params = new GetOperationsReport.Params(from, to);

    OperationsReport result = sut.get(params);

    assertEquals(10, result.finalizedOrders());
    verify(loadRepository).load(from, to);
  }

  @Test
  void throwsWhenToIsBeforeFrom() {
    GetOperationsReport.Params params = new GetOperationsReport.Params(to, from);

    assertThrows(IllegalArgumentException.class, () -> sut.get(params));
  }

  @Test
  void throwsWhenRangeExceeds365Days() {
    LocalDate farFuture = from.plusDays(366);
    GetOperationsReport.Params params = new GetOperationsReport.Params(from, farFuture);

    assertThrows(IllegalArgumentException.class, () -> sut.get(params));
  }

  @Test
  void allowsExactly365Days() {
    LocalDate exactly365 = from.plusDays(365);
    OperationsReport expected =
        new OperationsReport(
            from,
            exactly365,
            0,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            List.of(),
            List.of(),
            List.of());
    when(loadRepository.load(any(), any())).thenReturn(expected);

    GetOperationsReport.Params params = new GetOperationsReport.Params(from, exactly365);

    assertDoesNotThrow(() -> sut.get(params));
    verify(loadRepository).load(from, exactly365);
  }
}
