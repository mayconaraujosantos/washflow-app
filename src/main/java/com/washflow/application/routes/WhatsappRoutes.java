package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.AppConfig;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.modules.WhatsappModule;
import com.washflow.domain.usecases.whatsapp.WhatsappConnection;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import org.jdbi.v3.core.Jdbi;

public final class WhatsappRoutes {

  public static final String CONNECTION_PATH = "/api/whatsapp/connection";

  private WhatsappRoutes() {}

  public static void register(Jdbi jdbi, AppConfig appConfig) {
    registerStatus(appConfig);
    registerConnect(appConfig);
    registerDisconnect(appConfig);
  }

  @OpenApi(
      path = CONNECTION_PATH,
      methods = HttpMethod.GET,
      summary = "Is the lava-jato's WhatsApp number linked? (manager)",
      tags = {"WhatsApp"},
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = WhatsappConnection.ConnectionStatus.class)),
        @OpenApiResponse(status = "503", description = "WhatsApp not configured or unreachable")
      })
  private static void registerStatus(AppConfig appConfig) {
    get(
        CONNECTION_PATH,
        JavalinRouteAdapter.adapt(WhatsappModule.getConnectionController(appConfig)));
  }

  @OpenApi(
      path = CONNECTION_PATH,
      methods = HttpMethod.POST,
      summary = "QR code to link the lava-jato's WhatsApp number (manager)",
      tags = {"WhatsApp"},
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = WhatsappConnection.ConnectionStatus.class)),
        @OpenApiResponse(status = "503", description = "WhatsApp not configured or unreachable")
      })
  private static void registerConnect(AppConfig appConfig) {
    post(CONNECTION_PATH, JavalinRouteAdapter.adapt(WhatsappModule.connectController(appConfig)));
  }

  @OpenApi(
      path = CONNECTION_PATH,
      methods = HttpMethod.DELETE,
      summary = "Unlink the lava-jato's WhatsApp number (manager)",
      tags = {"WhatsApp"},
      responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = WhatsappConnection.ConnectionStatus.class)),
        @OpenApiResponse(status = "503", description = "WhatsApp not configured or unreachable")
      })
  private static void registerDisconnect(AppConfig appConfig) {
    delete(
        CONNECTION_PATH, JavalinRouteAdapter.adapt(WhatsappModule.disconnectController(appConfig)));
  }
}
