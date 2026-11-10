package com.washflow.data.protocols.db.cash;

import java.time.LocalDate;
import java.util.Map;

public interface CashTransactionReportRepository {

  Map<String, Object> report(LocalDate from, LocalDate to);
}
