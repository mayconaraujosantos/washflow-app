package com.washflow.data.usecases.getoperationsreport;

import com.washflow.data.protocols.db.LoadOperationsReportRepository;
import com.washflow.domain.usecases.GetOperationsReport;
import com.washflow.infra.db.jdbi.OperationsReport;
import java.time.temporal.ChronoUnit;

/**
 * Validates the date range and delegates to {@code LoadOperationsReportRepository}. Depends only on
 * the port interface, never on JDBI or Postgres.
 */
public class DbGetOperationsReport implements GetOperationsReport {

  private final LoadOperationsReportRepository loadRepository;

  public DbGetOperationsReport(LoadOperationsReportRepository loadRepository) {
    this.loadRepository = loadRepository;
  }

  @Override
  public OperationsReport get(Params params) {
    if (params.to().isBefore(params.from())) {
      throw new IllegalArgumentException(
          "Report range must be ordered and no longer than 366 days");
    }
    if (ChronoUnit.DAYS.between(params.from(), params.to()) > 365) {
      throw new IllegalArgumentException(
          "Report range must be ordered and no longer than 366 days");
    }
    return loadRepository.load(params.from(), params.to());
  }
}
