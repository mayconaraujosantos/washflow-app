package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.*;

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
import org.jdbi.v3.core.Jdbi;

public final class FeatureRoutes {
  private static final String SERVICE_ORDER_BY_ID = "/api/service-orders/{id}";
  private static final String CASH_TRANSACTIONS = "/api/cash-transactions";
  private static final String EXPENSE_CATEGORIES = "/api/expense-categories";

  private FeatureRoutes() {}

  public static void register(Jdbi jdbi) {
    // --- Checklists ---
    path(
        SERVICE_ORDER_BY_ID,
        () -> {
          post("/checklist", JavalinRouteAdapter.adapt(SaveChecklistControllerFactory.make(jdbi)));
          get("/checklist", JavalinRouteAdapter.adapt(GetChecklistsControllerFactory.make(jdbi)));

          // --- Photos ---
          post("/photos", JavalinRouteAdapter.adapt(SavePhotoControllerFactory.make(jdbi)));
          get("/photos", JavalinRouteAdapter.adapt(GetPhotosControllerFactory.make(jdbi)));
          delete(
              "/photos/{photoId}",
              JavalinRouteAdapter.adapt(DeletePhotoControllerFactory.make(jdbi)));
        });

    // --- Cash Transactions ---
    path(
        CASH_TRANSACTIONS,
        () -> {
          post("", JavalinRouteAdapter.adapt(CreateCashTransactionControllerFactory.make(jdbi)));
          get("", JavalinRouteAdapter.adapt(ListCashTransactionsControllerFactory.make(jdbi)));
          get(
              "/report",
              JavalinRouteAdapter.adapt(GetCashTransactionReportControllerFactory.make(jdbi)));
          put(
              "/{id}",
              JavalinRouteAdapter.adapt(UpdateCashTransactionControllerFactory.make(jdbi)));
          delete(
              "/{id}",
              JavalinRouteAdapter.adapt(DeleteCashTransactionControllerFactory.make(jdbi)));
        });

    // --- Expense Categories ---
    path(
        EXPENSE_CATEGORIES,
        () -> {
          post("", JavalinRouteAdapter.adapt(CreateExpenseCategoryControllerFactory.make(jdbi)));
          get("", JavalinRouteAdapter.adapt(ListExpenseCategoriesControllerFactory.make(jdbi)));
        });
  }
}
