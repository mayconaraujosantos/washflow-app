package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;

import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.GetOperationsReportControllerFactory;
import com.washflow.application.factories.controllers.GetWaitingQueueControllerFactory;
import com.washflow.application.factories.controllers.GetWashingByWasherControllerFactory;
import com.washflow.application.factories.controllers.GetYardOverviewControllerFactory;
import com.washflow.application.factories.controllers.PauseServiceOrderControllerFactory;
import com.washflow.application.factories.controllers.ResumeServiceOrderControllerFactory;
import org.jdbi.v3.core.Jdbi;

public final class OperationsRoutes {

  private OperationsRoutes() {}

  public static void register(Jdbi jdbi) {
    registerPause(jdbi);
    registerResume(jdbi);
    registerQueue(jdbi);
    registerYard(jdbi);
    registerReport(jdbi);
    registerMyOrders(jdbi);
  }

  private static void registerPause(Jdbi jdbi) {
    patch(
        "/api/service-orders/{id}/pause",
        JavalinRouteAdapter.adapt(PauseServiceOrderControllerFactory.make(jdbi)));
  }

  private static void registerResume(Jdbi jdbi) {
    patch(
        "/api/service-orders/{id}/resume",
        JavalinRouteAdapter.adapt(ResumeServiceOrderControllerFactory.make(jdbi)));
  }

  private static void registerQueue(Jdbi jdbi) {
    get(
        "/api/operations/queue",
        JavalinRouteAdapter.adapt(GetWaitingQueueControllerFactory.make(jdbi)));
  }

  private static void registerYard(Jdbi jdbi) {
    get(
        "/api/operations/yard",
        JavalinRouteAdapter.adapt(GetYardOverviewControllerFactory.make(jdbi)));
  }

  private static void registerReport(Jdbi jdbi) {
    get(
        "/api/operations/report",
        JavalinRouteAdapter.adapt(GetOperationsReportControllerFactory.make(jdbi)));
  }

  private static void registerMyOrders(Jdbi jdbi) {
    get(
        "/api/operations/my-orders",
        JavalinRouteAdapter.adapt(GetWashingByWasherControllerFactory.make(jdbi)));
  }
}
