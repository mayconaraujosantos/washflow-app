package com.washflow.data.usecases.getcashtransactionreport;

import com.washflow.data.protocols.db.CashTransactionReportRepository;
import com.washflow.domain.usecases.GetCashTransactionReport;
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
