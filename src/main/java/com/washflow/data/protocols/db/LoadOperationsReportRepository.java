package com.washflow.data.protocols.db;

import com.washflow.infra.db.jdbi.OperationsReport;
import java.time.LocalDate;

/** Port for loading an operations report for a date range. */
public interface LoadOperationsReportRepository {

  OperationsReport load(LocalDate from, LocalDate to);
}
