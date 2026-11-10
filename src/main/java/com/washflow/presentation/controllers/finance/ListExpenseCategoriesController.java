package com.washflow.presentation.controllers.finance;

import com.washflow.domain.entities.finance.ExpenseCategory;
import com.washflow.domain.usecases.finance.ListExpenseCategories;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;

public class ListExpenseCategoriesController implements Controller {

  private final ListExpenseCategories listExpenseCategories;

  public ListExpenseCategoriesController(ListExpenseCategories listExpenseCategories) {
    this.listExpenseCategories = listExpenseCategories;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      List<ExpenseCategory> categories = listExpenseCategories.list();
      return HttpHelper.ok(categories);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
