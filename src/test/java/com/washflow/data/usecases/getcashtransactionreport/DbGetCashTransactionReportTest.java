package com.washflow.data.usecases.getcashtransactionreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.CashTransactionReportRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbGetCashTransactionReportTest {

  @Mock private CashTransactionReportRepository reportRepository;

  private DbGetCashTransactionReport sut;

  @BeforeEach
  void setUp() {
    sut = new DbGetCashTransactionReport(reportRepository);
  }

  @Test
  void returnsReport() {
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate to = LocalDate.of(2025, 1, 31);
    Map<String, Object> expected = new HashMap<>();
    expected.put("totalIncome", 1000.0);
    expected.put("totalExpense", 300.0);
    when(reportRepository.report(eq(from), eq(to))).thenReturn(expected);

    Map<String, Object> result = sut.report(from, to);

    assertEquals(1000.0, result.get("totalIncome"));
    verify(reportRepository).report(from, to);
  }
}
