package com.washflow.domain.usecases;

import com.washflow.domain.entities.CashTransaction;
import java.time.LocalDate;
import java.util.List;

public interface ListCashTransactions {

  List<CashTransaction> list(LocalDate from, LocalDate to);

  List<CashTransaction> listAll();
}
