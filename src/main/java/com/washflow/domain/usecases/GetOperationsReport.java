package com.washflow.domain.usecases;

import com.washflow.infra.db.jdbi.OperationsReport;
import java.time.LocalDate;

/**
 * Returns an operations report for a date range. Implemented by {@code DbGetOperationsReport} in
 * the data layer.
 */
public interface GetOperationsReport {

  OperationsReport get(Params params);

  record Params(LocalDate from, LocalDate to) {}
}
