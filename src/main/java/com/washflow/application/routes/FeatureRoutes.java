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
import org.jdbi.v3.core.Jdbi;

public final class FeatureRoutes {

  private FeatureRoutes() {}

  public static void register(Jdbi jdbi) {
    // --- Checklists (Clean Architecture pattern) ---
    post("/api/service-orders/{id}/checklist",
         JavalinRouteAdapter.adapt(SaveChecklistControllerFactory.make(jdbi)));
    get("/api/service-orders/{id}/checklist",
        JavalinRouteAdapter.adapt(GetChecklistsControllerFactory.make(jdbi)));

    // --- Photos (Clean Architecture pattern) ---
    post("/api/service-orders/{id}/photos",
         JavalinRouteAdapter.adapt(SavePhotoControllerFactory.make(jdbi)));
    get("/api/service-orders/{id}/photos",
        JavalinRouteAdapter.adapt(GetPhotosControllerFactory.make(jdbi)));
    delete("/api/service-orders/{id}/photos/{photoId}",
           JavalinRouteAdapter.adapt(DeletePhotoControllerFactory.make(jdbi)));

    // --- Cash Transactions (Clean Architecture pattern) ---
    post("/api/cash-transactions",
         JavalinRouteAdapter.adapt(CreateCashTransactionControllerFactory.make(jdbi)));
    get("/api/cash-transactions",
        JavalinRouteAdapter.adapt(ListCashTransactionsControllerFactory.make(jdbi)));
    put("/api/cash-transactions/{id}",
        JavalinRouteAdapter.adapt(UpdateCashTransactionControllerFactory.make(jdbi)));
    delete("/api/cash-transactions/{id}",
           JavalinRouteAdapter.adapt(DeleteCashTransactionControllerFactory.make(jdbi)));
    get("/api/cash-transactions/report",
        JavalinRouteAdapter.adapt(GetCashTransactionReportControllerFactory.make(jdbi)));

    // --- Expense Categories (Clean Architecture pattern) ---
    post("/api/expense-categories",
         JavalinRouteAdapter.adapt(CreateExpenseCategoryControllerFactory.make(jdbi)));
    get("/api/expense-categories",
        JavalinRouteAdapter.adapt(ListExpenseCategoriesControllerFactory.make(jdbi)));
  }
}
