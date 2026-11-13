package com.washflow.application.modules;

import com.washflow.application.AppConfig;
import com.washflow.data.protocols.ai.MessageInterpreter;
import com.washflow.data.protocols.messaging.WhatsappGateway;
import com.washflow.data.usecases.schedule.DbScheduleAvailability;
import com.washflow.data.usecases.whatsapp.DbReceiveWhatsappMessage;
import com.washflow.data.usecases.whatsapp.DbSendScheduleReminders;
import com.washflow.data.usecases.whatsapp.DbWhatsappConnection;
import com.washflow.data.usecases.whatsapp.NotifyingCompleteServiceOrder;
import com.washflow.data.usecases.whatsapp.WhatsappAssistant;
import com.washflow.data.usecases.whatsapp.WhatsappNotices;
import com.washflow.domain.usecases.serviceorder.CompleteServiceOrder;
import com.washflow.domain.usecases.whatsapp.ReceiveWhatsappMessage;
import com.washflow.domain.usecases.whatsapp.SendScheduleReminders;
import com.washflow.domain.usecases.whatsapp.WhatsappConnection;
import com.washflow.infra.ai.ClaudeMessageInterpreter;
import com.washflow.infra.db.jdbi.serviceorder.AgendaJdbiRepository;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.db.jdbi.whatsapp.ConversationJdbiRepository;
import com.washflow.infra.db.jdbi.whatsapp.NotificationJdbiRepository;
import com.washflow.infra.messaging.EvolutionWhatsappGateway;
import com.washflow.presentation.controllers.whatsapp.ConnectWhatsappController;
import com.washflow.presentation.controllers.whatsapp.DisconnectWhatsappController;
import com.washflow.presentation.controllers.whatsapp.GetWhatsappConnectionController;
import com.washflow.presentation.controllers.whatsapp.WhatsappWebhookController;
import com.washflow.presentation.protocols.Controller;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jdbi.v3.core.Jdbi;

/**
 * Composition root of each lava-jato's WhatsApp: number linking, status notices, reminders and the
 * scheduling assistant. Everything is off (no gateway) while WHATSAPP_WEBHOOK_URL isn't set.
 */
public final class WhatsappModule {

  // Notices leave the washer's request right away; two threads are plenty for text messages.
  private static final ExecutorService NOTICES =
      Executors.newFixedThreadPool(
          2,
          runnable -> {
            Thread thread = new Thread(runnable, "whatsapp-notices");
            thread.setDaemon(true);
            return thread;
          });

  private WhatsappModule() {}

  /** Null when the server has no WhatsApp provider configured. */
  public static WhatsappGateway gateway(AppConfig appConfig) {
    if (!appConfig.whatsapp().enabled()) {
      return null;
    }
    return new EvolutionWhatsappGateway(appConfig.otp().evolution());
  }

  public static WhatsappGateway.Webhook webhook(AppConfig appConfig) {
    return new WhatsappGateway.Webhook(
        appConfig.whatsapp().webhookUrl(), appConfig.whatsapp().webhookToken());
  }

  public static WhatsappConnection connection(AppConfig appConfig) {
    return new DbWhatsappConnection(gateway(appConfig), webhook(appConfig));
  }

  static WhatsappNotices notices(Jdbi jdbi, WhatsappGateway gateway) {
    return new WhatsappNotices(new NotificationJdbiRepository(jdbi), gateway, NOTICES);
  }

  /** The washer's "pronto" also tells the customer, when the server has WhatsApp. */
  public static CompleteServiceOrder notifying(
      CompleteServiceOrder completeServiceOrder, Jdbi jdbi, AppConfig appConfig) {
    WhatsappGateway gateway = gateway(appConfig);
    if (gateway == null) {
      return completeServiceOrder;
    }
    return new NotifyingCompleteServiceOrder(completeServiceOrder, notices(jdbi, gateway));
  }

  /** Null when the server has no WhatsApp provider configured. */
  public static SendScheduleReminders scheduleReminders(Jdbi jdbi, AppConfig appConfig) {
    WhatsappGateway gateway = gateway(appConfig);
    if (gateway == null) {
      return null;
    }
    return new DbSendScheduleReminders(
        new NotificationJdbiRepository(jdbi), notices(jdbi, gateway), Clock.systemUTC());
  }

  /**
   * The assistant drives the same use cases as the customer's screens; {@code interpreter} adds
   * free-text understanding when the server has an AI key.
   */
  public static WhatsappAssistant assistant(Jdbi jdbi, AppConfig appConfig) {
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    return new WhatsappAssistant(
        new WhatsappAssistant.Tools(
            PricingModule.listServicePrices(jdbi),
            VehicleModule.listVehiclesByCustomer(jdbi),
            VehicleModule.createVehicle(jdbi),
            new DbScheduleAvailability(
                EstablishmentHours.supplier(jdbi, appConfig),
                new AgendaJdbiRepository(jdbi),
                Clock.systemUTC()),
            ScheduleModule.scheduleServiceOrder(jdbi, appConfig),
            CustomerModule.listServiceOrdersByCustomer(jdbi),
            ServiceOrderModule.cancelServiceOrder(jdbi),
            users),
        interpreter(appConfig));
  }

  /** Claude reads free text when ANTHROPIC_API_KEY is set; null keeps the numbered menus only. */
  static MessageInterpreter interpreter(AppConfig appConfig) {
    if (!appConfig.assistantAi().enabled()) {
      return null;
    }
    return new ClaudeMessageInterpreter(
        appConfig.assistantAi().apiKey(), appConfig.assistantAi().model());
  }

  /** One instance per server: it serializes each phone's messages. */
  public static ReceiveWhatsappMessage receiveMessage(Jdbi jdbi, AppConfig appConfig) {
    return new DbReceiveWhatsappMessage(
        new ConversationJdbiRepository(jdbi),
        new UserJdbiRepository(jdbi),
        assistant(jdbi, appConfig),
        gateway(appConfig),
        Clock.systemUTC());
  }

  /** Forgets received message ids after a week: resent webhooks arrive within minutes. */
  public static Runnable housekeeping(Jdbi jdbi) {
    ConversationJdbiRepository conversations = new ConversationJdbiRepository(jdbi);
    return () -> conversations.forgetReceivedBefore(Instant.now().minus(Duration.ofDays(7)));
  }

  public static Controller webhookController(Jdbi jdbi, AppConfig appConfig) {
    return new WhatsappWebhookController(receiveMessage(jdbi, appConfig));
  }

  public static Controller getConnectionController(AppConfig appConfig) {
    return new GetWhatsappConnectionController(connection(appConfig));
  }

  public static Controller connectController(AppConfig appConfig) {
    return new ConnectWhatsappController(connection(appConfig));
  }

  public static Controller disconnectController(AppConfig appConfig) {
    return new DisconnectWhatsappController(connection(appConfig));
  }
}
