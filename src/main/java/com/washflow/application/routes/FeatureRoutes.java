package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.CreateCashTransactionControllerFactory;
import com.washflow.application.factories.controllers.CreateExpenseCategoryControllerFactory;
import com.washflow.application.factories.controllers.DeleteCashTransactionControllerFactory;
import com.washflow.application.factories.controllers.DeletePhotoControllerFactory;
import com.washflow.application.factories.controllers.GetCashTransactionReportControllerFactory;
import com.washflow.application.factories.controllers.GetChecklistsControllerFactory;
import com.washflow.application.factories.controllers.GetPhotosControllerFactory;
import com.washflow.application.factories.controllers.ListCashTransactionsControllerFactory;
import com.washflow.application.factories.controllers.ListExpenseCategoriesControllerFactory;
import com.washflow.application.factories.controllers.SaveChecklistControllerFactory;
import com.washflow.application.factories.controllers.SavePhotoControllerFactory;
import com.washflow.application.factories.controllers.UpdateCashTransactionControllerFactory;
import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.ExpenseCategory;
import com.washflow.domain.entities.ServiceOrderChecklist;
import com.washflow.domain.entities.ServiceOrderPhoto;
import com.washflow.infra.db.jdbi.OperationsReport;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

public final class FeatureRoutes {

  private static final String SERVICE_ORDER_BY_ID = "/api/service-orders/{id}";
  private static final String CASH_TRANSACTIONS = "/api/cash-transactions";
  private static final String EXPENSE_CATEGORIES = "/api/expense-categories";

  private FeatureRoutes() {}

  public static void register(Jdbi jdbi) {
    registerChecklists(jdbi);
    registerPhotos(jdbi);
    registerCashTransactions(jdbi);
    registerExpenseCategories(jdbi);
  }

  // --- Checklists ---

  @OpenApi(
      path = SERVICE_ORDER_BY_ID + "/checklist",
      methods = HttpMethod.POST,
      summary = "Save a checklist (entry or exit) for a service order",
      tags = {"Checklists"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(
            status = "201",
            content = @OpenApiContent(from = ServiceOrderChecklist.class)),
        @OpenApiResponse(status = "400", description = "Missing or invalid field"),
        @OpenApiResponse(status = "404", description = "Service order not found")
      })
  private static void registerSaveChecklist(Jdbi jdbi) {
    post(
        SERVICE_ORDER_BY_ID + "/checklist",
        JavalinRouteAdapter.adapt(SaveChecklistControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = SERVICE_ORDER_BY_ID + "/checklist",
      methods = HttpMethod.GET,
      summary = "List checklists for a service order",
      tags = {"Checklists"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = ServiceOrderChecklist[].class)),
        @OpenApiResponse(status = "400", description = "Malformed service order id")
      })
  private static void registerGetChecklists(Jdbi jdbi) {
    get(
        SERVICE_ORDER_BY_ID + "/checklist",
        JavalinRouteAdapter.adapt(GetChecklistsControllerFactory.make(jdbi)));
  }

  private static void registerChecklists(Jdbi jdbi) {
    registerSaveChecklist(jdbi);
    registerGetChecklists(jdbi);
  }

  // --- Photos ---

  @OpenApi(
      path = SERVICE_ORDER_BY_ID + "/photos",
      methods = HttpMethod.POST,
      summary = "Upload a photo (entry or exit) for a service order",
      tags = {"Photos"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "201", content = @OpenApiContent(from = ServiceOrderPhoto.class)),
        @OpenApiResponse(status = "400", description = "Missing or invalid field"),
        @OpenApiResponse(status = "404", description = "Service order not found")
      })
  private static void registerSavePhoto(Jdbi jdbi) {
    post(
        SERVICE_ORDER_BY_ID + "/photos",
        JavalinRouteAdapter.adapt(SavePhotoControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = SERVICE_ORDER_BY_ID + "/photos",
      methods = HttpMethod.GET,
      summary = "List photos for a service order",
      tags = {"Photos"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = ServiceOrderPhoto[].class)),
        @OpenApiResponse(status = "400", description = "Malformed service order id")
      })
  private static void registerGetPhotos(Jdbi jdbi) {
    get(
        SERVICE_ORDER_BY_ID + "/photos",
        JavalinRouteAdapter.adapt(GetPhotosControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = SERVICE_ORDER_BY_ID + "/photos/{photoId}",
      methods = HttpMethod.DELETE,
      summary = "Delete a photo from a service order",
      tags = {"Photos"},
      pathParams = {
        @OpenApiParam(name = "id", type = String.class, required = true),
        @OpenApiParam(name = "photoId", type = String.class, required = true)
      },
      responses = {
        @OpenApiResponse(status = "200", description = "Photo deleted"),
        @OpenApiResponse(status = "400", description = "Malformed id"),
        @OpenApiResponse(status = "404", description = "Photo not found")
      })
  private static void registerDeletePhoto(Jdbi jdbi) {
    delete(
        SERVICE_ORDER_BY_ID + "/photos/{photoId}",
        JavalinRouteAdapter.adapt(DeletePhotoControllerFactory.make(jdbi)));
  }

  private static void registerPhotos(Jdbi jdbi) {
    registerSavePhoto(jdbi);
    registerGetPhotos(jdbi);
    registerDeletePhoto(jdbi);
  }

  // --- Cash Transactions ---

  @OpenApi(
      path = CASH_TRANSACTIONS,
      methods = HttpMethod.POST,
      summary = "Create a cash transaction (income or expense)",
      tags = {"Cash Transactions"},
      responses = {
        @OpenApiResponse(status = "201", content = @OpenApiContent(from = CashTransaction.class)),
        @OpenApiResponse(status = "400", description = "Missing or invalid field")
      })
  private static void registerCreateCashTransaction(Jdbi jdbi) {
    post(
        CASH_TRANSACTIONS,
        JavalinRouteAdapter.adapt(CreateCashTransactionControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = CASH_TRANSACTIONS,
      methods = HttpMethod.GET,
      summary = "List cash transactions, optionally filtered by date range",
      tags = {"Cash Transactions"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = CashTransaction[].class)),
        @OpenApiResponse(status = "400", description = "Invalid date range")
      })
  private static void registerListCashTransactions(Jdbi jdbi) {
    get(
        CASH_TRANSACTIONS,
        JavalinRouteAdapter.adapt(ListCashTransactionsControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = CASH_TRANSACTIONS + "/report",
      methods = HttpMethod.GET,
      summary = "Get a financial report summarizing income and expenses by category",
      tags = {"Cash Transactions"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = OperationsReport.class)),
        @OpenApiResponse(status = "400", description = "Invalid date range")
      })
  private static void registerGetCashTransactionReport(Jdbi jdbi) {
    get(
        CASH_TRANSACTIONS + "/report",
        JavalinRouteAdapter.adapt(GetCashTransactionReportControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = CASH_TRANSACTIONS + "/{id}",
      methods = HttpMethod.PUT,
      summary = "Partially update a cash transaction",
      tags = {"Cash Transactions"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = CashTransaction.class)),
        @OpenApiResponse(status = "400", description = "Invalid field"),
        @OpenApiResponse(status = "404", description = "Transaction not found")
      })
  private static void registerUpdateCashTransaction(Jdbi jdbi) {
    put(
        CASH_TRANSACTIONS + "/{id}",
        JavalinRouteAdapter.adapt(UpdateCashTransactionControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = CASH_TRANSACTIONS + "/{id}",
      methods = HttpMethod.DELETE,
      summary = "Delete a cash transaction",
      tags = {"Cash Transactions"},
      pathParams = @OpenApiParam(name = "id", type = String.class, required = true),
      responses = {
        @OpenApiResponse(status = "200", description = "Transaction deleted"),
        @OpenApiResponse(status = "400", description = "Malformed id"),
        @OpenApiResponse(status = "404", description = "Transaction not found")
      })
  private static void registerDeleteCashTransaction(Jdbi jdbi) {
    delete(
        CASH_TRANSACTIONS + "/{id}",
        JavalinRouteAdapter.adapt(DeleteCashTransactionControllerFactory.make(jdbi)));
  }

  private static void registerCashTransactions(Jdbi jdbi) {
    registerCreateCashTransaction(jdbi);
    registerListCashTransactions(jdbi);
    registerGetCashTransactionReport(jdbi);
    registerUpdateCashTransaction(jdbi);
    registerDeleteCashTransaction(jdbi);
  }

  // --- Expense Categories ---

  @OpenApi(
      path = EXPENSE_CATEGORIES,
      methods = HttpMethod.POST,
      summary = "Create an expense category",
      tags = {"Expense Categories"},
      responses = {
        @OpenApiResponse(status = "201", content = @OpenApiContent(from = ExpenseCategory.class)),
        @OpenApiResponse(status = "400", description = "Missing name")
      })
  private static void registerCreateExpenseCategory(Jdbi jdbi) {
    post(
        EXPENSE_CATEGORIES,
        JavalinRouteAdapter.adapt(CreateExpenseCategoryControllerFactory.make(jdbi)));
  }

  @OpenApi(
      path = EXPENSE_CATEGORIES,
      methods = HttpMethod.GET,
      summary = "List all expense categories",
      tags = {"Expense Categories"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = ExpenseCategory[].class))
      })
  private static void registerListExpenseCategories(Jdbi jdbi) {
    get(
        EXPENSE_CATEGORIES,
        JavalinRouteAdapter.adapt(ListExpenseCategoriesControllerFactory.make(jdbi)));
  }

  private static void registerExpenseCategories(Jdbi jdbi) {
    registerCreateExpenseCategory(jdbi);
    registerListExpenseCategories(jdbi);
  }
}
