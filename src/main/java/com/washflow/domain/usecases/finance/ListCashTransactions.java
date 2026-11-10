package com.washflow.domain.usecases.finance;

import com.washflow.domain.entities.finance.CashTransaction;
import java.time.LocalDate;
import java.util.List;

public interface ListCashTransactions {

  List<CashTransaction> list(LocalDate from, LocalDate to);

  List<CashTransaction> listAll();
}
