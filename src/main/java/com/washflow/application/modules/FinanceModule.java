package com.washflow.application.modules;

import com.washflow.data.usecases.finance.DbCreateCashTransaction;
import com.washflow.data.usecases.finance.DbCreateExpenseCategory;
import com.washflow.data.usecases.finance.DbDeleteCashTransaction;
import com.washflow.data.usecases.finance.DbGetCashTransactionReport;
import com.washflow.data.usecases.finance.DbGetOperationsReport;
import com.washflow.data.usecases.finance.DbListCashTransactions;
import com.washflow.data.usecases.finance.DbListExpenseCategories;
import com.washflow.data.usecases.finance.DbUpdateCashTransaction;
import com.washflow.domain.usecases.finance.CreateCashTransaction;
import com.washflow.domain.usecases.finance.CreateExpenseCategory;
import com.washflow.domain.usecases.finance.DeleteCashTransaction;
import com.washflow.domain.usecases.finance.GetCashTransactionReport;
import com.washflow.domain.usecases.finance.GetOperationsReport;
import com.washflow.domain.usecases.finance.ListCashTransactions;
import com.washflow.domain.usecases.finance.ListExpenseCategories;
import com.washflow.domain.usecases.finance.UpdateCashTransaction;
import com.washflow.infra.db.jdbi.finance.CashTransactionJdbiRepository;
import com.washflow.infra.db.jdbi.finance.OperationsReportJdbiRepository;
import com.washflow.presentation.controllers.finance.CreateCashTransactionController;
import com.washflow.presentation.controllers.finance.CreateExpenseCategoryController;
import com.washflow.presentation.controllers.finance.DeleteCashTransactionController;
import com.washflow.presentation.controllers.finance.GetCashTransactionReportController;
import com.washflow.presentation.controllers.finance.GetOperationsReportController;
import com.washflow.presentation.controllers.finance.ListCashTransactionsController;
import com.washflow.presentation.controllers.finance.ListExpenseCategoriesController;
import com.washflow.presentation.controllers.finance.UpdateCashTransactionController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

/** Composition root of the cash register and the reports: builds its use cases and controllers. */
public final class FinanceModule {

  private FinanceModule() {}

  public static CreateCashTransaction createCashTransaction(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbCreateCashTransaction(repository);
  }

  public static CreateExpenseCategory createExpenseCategory(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbCreateExpenseCategory(repository);
  }

  public static DeleteCashTransaction deleteCashTransaction(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbDeleteCashTransaction(repository);
  }

  public static GetCashTransactionReport getCashTransactionReport(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbGetCashTransactionReport(repository);
  }

  public static GetOperationsReport getOperationsReport(Jdbi jdbi) {
    OperationsReportJdbiRepository repository = new OperationsReportJdbiRepository(jdbi);
    return new DbGetOperationsReport(repository);
  }

  public static ListCashTransactions listCashTransactions(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbListCashTransactions(repository);
  }

  public static ListExpenseCategories listExpenseCategories(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbListExpenseCategories(repository);
  }

  public static UpdateCashTransaction updateCashTransaction(Jdbi jdbi) {
    CashTransactionJdbiRepository repository = new CashTransactionJdbiRepository(jdbi);
    return new DbUpdateCashTransaction(repository, repository);
  }

  public static Controller createCashTransactionController(Jdbi jdbi) {
    return new CreateCashTransactionController(createCashTransaction(jdbi));
  }

  public static Controller createExpenseCategoryController(Jdbi jdbi) {
    return new CreateExpenseCategoryController(createExpenseCategory(jdbi));
  }

  public static Controller deleteCashTransactionController(Jdbi jdbi) {
    return new DeleteCashTransactionController(deleteCashTransaction(jdbi));
  }

  public static Controller getCashTransactionReportController(Jdbi jdbi) {
    return new GetCashTransactionReportController(getCashTransactionReport(jdbi));
  }

  public static Controller getOperationsReportController(Jdbi jdbi) {
    return new GetOperationsReportController(getOperationsReport(jdbi));
  }

  public static Controller listCashTransactionsController(Jdbi jdbi) {
    return new ListCashTransactionsController(listCashTransactions(jdbi));
  }

  public static Controller listExpenseCategoriesController(Jdbi jdbi) {
    return new ListExpenseCategoriesController(listExpenseCategories(jdbi));
  }

  public static Controller updateCashTransactionController(Jdbi jdbi) {
    return new UpdateCashTransactionController(updateCashTransaction(jdbi));
  }
}
