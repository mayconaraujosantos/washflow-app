package com.washflow.data.usecases.cash;

import com.washflow.data.protocols.db.cash.CashTransactionReportRepository;
import com.washflow.domain.usecases.cash.GetCashTransactionReport;
import java.time.LocalDate;
import java.util.Map;

public class DbGetCashTransactionReport implements GetCashTransactionReport {

  private final CashTransactionReportRepository reportRepository;

  public DbGetCashTransactionReport(CashTransactionReportRepository reportRepository) {
    this.reportRepository = reportRepository;
  }

  @Override
  public Map<String, Object> report(LocalDate from, LocalDate to) {
    return reportRepository.report(from, to);
  }
}
