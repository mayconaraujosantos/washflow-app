package com.washflow.data.protocols.db.finance;

import com.washflow.domain.entities.OperationsReport;
import java.time.LocalDate;

/** Port for loading an operations report for a date range. */
public interface LoadOperationsReportRepository {

  OperationsReport load(LocalDate from, LocalDate to);
}
