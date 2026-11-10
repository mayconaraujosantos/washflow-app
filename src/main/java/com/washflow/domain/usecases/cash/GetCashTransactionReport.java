package com.washflow.domain.usecases.cash;

import java.time.LocalDate;
import java.util.Map;

public interface GetCashTransactionReport {

  Map<String, Object> report(LocalDate from, LocalDate to);
}
