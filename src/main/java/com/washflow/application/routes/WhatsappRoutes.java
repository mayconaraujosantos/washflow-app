package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import com.washflow.application.AppConfig;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.modules.WhatsappModule;
import com.washflow.application.security.AuthorizationMiddleware;
import com.washflow.domain.usecases.whatsapp.WhatsappConnection;
import com.washflow.infra.messaging.EvolutionWhatsappGateway;
import com.washflow.presentation.helpers.HttpHelper;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.jdbi.v3.core.Jdbi;

public final class WhatsappRoutes {

  public static final String CONNECTION_PATH = "/api/whatsapp/connection";

  private WhatsappRoutes() {}

  public static void register(Jdbi jdbi, AppConfig appConfig) {
    registerStatus(appConfig);
    registerConnect(appConfig);
    registerDisconnect(appConfig);
    if (appConfig.whatsapp().enabled()) {
      registerWebhook(jdbi, appConfig);
    }
  }

  @OpenApi(
      path = AuthorizationMiddleware.WEBHOOK_PATH,
      methods = HttpMethod.POST,
      summary = "Messages received by the lava-jatos' WhatsApp numbers (Evolution API webhook)",
      tags = {"WhatsApp"},
      responses = {
        @OpenApiResponse(status = "200", description = "Received (or ignored)"),
        @OpenApiResponse(status = "401", description = "Missing or wrong webhook token")
      })
  private static void registerWebhook(Jdbi jdbi, AppConfig appConfig) {
    Handler handler = JavalinRouteAdapter.adapt(WhatsappModule.webhookController(jdbi, appConfig));
    byte[] expected = appConfig.whatsapp().webhookToken().getBytes(StandardCharsets.UTF_8);
    post(
        AuthorizationMiddleware.WEBHOOK_PATH,
        ctx -> {
          String token = ctx.header(EvolutionWhatsappGateway.TOKEN_HEADER);
          // Constant time: the comparison must not tell how much of a guess was right.
          if (token == null
              || !MessageDigest.isEqual(expected, token.getBytes(StandardCharsets.UTF_8))) {
            ctx.status(401).json(HttpHelper.errorBody("Invalid webhook token"));
            return;
          }
          handler.handle(ctx);
        });
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
