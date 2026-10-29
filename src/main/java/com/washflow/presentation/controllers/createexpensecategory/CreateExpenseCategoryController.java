package com.washflow.presentation.controllers.createexpensecategory;

import com.washflow.domain.usecases.CreateExpenseCategory;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

public class CreateExpenseCategoryController implements Controller {

  private final CreateExpenseCategory createExpenseCategory;

  public CreateExpenseCategoryController(CreateExpenseCategory createExpenseCategory) {
    this.createExpenseCategory = createExpenseCategory;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body();
      String name = (String) body.get("name");
      if (name == null || name.isBlank()) {
        return HttpHelper.badRequest(new IllegalArgumentException("name is required"));
      }
      String color = (String) body.get("color");
      if (color == null || color.isBlank()) {
        color = "#6B7280";
      }
      CreateExpenseCategory.Params params = new CreateExpenseCategory.Params(name, color);
      var saved = createExpenseCategory.create(params);
      return new HttpResponse(201, saved);
    } catch (Exception e) {
      return HttpHelper.badRequest(e);
    }
  }
}
