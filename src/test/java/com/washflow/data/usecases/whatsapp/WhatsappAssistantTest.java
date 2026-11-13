package com.washflow.data.usecases.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.ai.MessageInterpreter;
import com.washflow.data.protocols.db.user.CreateUserRepository;
import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.domain.entities.pricing.ServicePrice;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.entities.vehicle.Vehicle;
import com.washflow.domain.entities.whatsapp.Conversation;
import com.washflow.domain.entities.whatsapp.ConversationStep;
import com.washflow.domain.errors.schedule.SlotFullyBookedError;
import com.washflow.domain.usecases.customer.ListServiceOrdersByCustomer;
import com.washflow.domain.usecases.pricing.ListServicePrices;
import com.washflow.domain.usecases.schedule.ScheduleAvailability;
import com.washflow.domain.usecases.schedule.ScheduleServiceOrder;
import com.washflow.domain.usecases.serviceorder.CancelServiceOrder;
import com.washflow.domain.usecases.vehicle.CreateVehicle;
import com.washflow.domain.usecases.vehicle.ListVehiclesByCustomer;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WhatsappAssistantTest {

  // Friday 20/11, 08:00 in Manaus.
  private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
  private static final LocalDate TODAY = LocalDate.parse("2026-11-20");
  private static final User ANA =
      new User(UUID.randomUUID(), "Ana Souza", "92991234567", UserProfile.CUSTOMER, NOW, true);
  private static final Vehicle ONIX =
      new Vehicle(UUID.randomUUID(), ANA.id(), "ABC1D23", "Onix", "Prata");
  private static final ServicePrice SIMPLE =
      new ServicePrice(1, "Lavagem Simples", new BigDecimal("40.00"), new BigDecimal("10.00"));
  private static final ServicePrice WAX =
      new ServicePrice(2, "Cera", new BigDecimal("25.00"), new BigDecimal("5.00"));

  private final ListServicePrices prices = mock(ListServicePrices.class);
  private final ListVehiclesByCustomer vehicles = mock(ListVehiclesByCustomer.class);
  private final CreateVehicle createVehicle = mock(CreateVehicle.class);
  private final ScheduleAvailability availability = mock(ScheduleAvailability.class);
  private final ScheduleServiceOrder schedule = mock(ScheduleServiceOrder.class);
  private final ListServiceOrdersByCustomer orders = mock(ListServiceOrdersByCustomer.class);
  private final CancelServiceOrder cancel = mock(CancelServiceOrder.class);
  private final CreateUserRepository createUser = mock(CreateUserRepository.class);
  private final MessageInterpreter interpreter = mock(MessageInterpreter.class);

  private WhatsappAssistant assistant;
  private Conversation conversation;
  private User customer;
  private Instant now;

  @BeforeEach
  void setUp() {
    when(prices.list()).thenReturn(List.of(SIMPLE, WAX));
    when(vehicles.list(any())).thenReturn(List.of(ONIX));
    when(orders.list(any())).thenReturn(List.of());
    // Open every day, 08:00-18:00 shop-local, everything free.
    when(availability.forDate(any()))
        .thenAnswer(
            call -> {
              LocalDate day = call.getArgument(0);
              List<ScheduleAvailability.Slot> slots =
                  IntStream.range(8, 18)
                      .mapToObj(
                          hour ->
                              new ScheduleAvailability.Slot(
                                  day.atTime(hour, 0)
                                      .atZone(SchedulingRules.STORE_ZONE)
                                      .toInstant(),
                                  hour,
                                  0,
                                  3,
                                  true))
                      .toList();
              return new ScheduleAvailability.Day(day, true, slots);
            });
    assistant = withoutAi();
    customer = ANA;
    now = NOW;
  }

  private WhatsappAssistant withoutAi() {
    return new WhatsappAssistant(
        new WhatsappAssistant.Tools(
            prices, vehicles, createVehicle, availability, schedule, orders, cancel, createUser),
        null);
  }

  private List<String> say(String text) {
    WhatsappAssistant.Turn turn =
        assistant.reply(
            new WhatsappAssistant.Contact("Lava Jato Central", "92991234567", customer),
            conversation,
            text,
            now);
    conversation = turn.next();
    customer = turn.customer();
    return turn.messages();
  }

  private static String last(List<String> messages) {
    return messages.get(messages.size() - 1);
  }

  @Test
  void anUnknownNumberIsAskedItsNameAndGetsAnAccount() {
    customer = null;
    when(createUser.create(any(), any())).thenAnswer(call -> call.getArgument(0));

    assertTrue(say("oi").get(0).contains("como podemos te chamar"));
    List<String> replies = say("Ana Souza");

    ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
    verify(createUser).create(created.capture(), any());
    assertEquals("Ana Souza", created.getValue().name());
    assertEquals("92991234567", created.getValue().phone());
    assertEquals(UserProfile.CUSTOMER, created.getValue().profile());
    assertEquals("Prazer, Ana!", replies.get(0));
    assertTrue(replies.get(1).contains("1. Agendar lavagem"));
    assertEquals(ConversationStep.MENU, conversation.step());
  }

  @Test
  void schedulesThroughTheMenus() {
    say("oi");
    assertTrue(last(say("1")).contains("1. Onix prata (ABC1D23)"));
    assertTrue(last(say("1")).contains("2. Cera — R$ 25,00"));
    assertTrue(last(say("1, 2")).contains("1. Hoje (sex 20/11)"));
    String times = last(say("2"));
    assertTrue(times.startsWith("Horários livres amanhã"), times);
    String confirm = last(say("1"));
    assertTrue(confirm.contains("Lavagem Simples + Cera"), confirm);
    assertTrue(confirm.contains("R$ 65,00"), confirm);
    assertTrue(confirm.contains("amanhã (sáb 21/11) às 08:00"), confirm);

    String done = last(say("1"));

    ArgumentCaptor<ScheduleServiceOrder.Params> booked =
        ArgumentCaptor.forClass(ScheduleServiceOrder.Params.class);
    verify(schedule).schedule(booked.capture());
    assertEquals(ONIX.id(), booked.getValue().vehicleId());
    assertEquals(List.of(1, 2), booked.getValue().servicePriceIds());
    assertEquals(Instant.parse("2026-11-21T12:00:00Z"), booked.getValue().scheduledAt());
    assertTrue(done.startsWith("Agendado ✅"), done);
    assertEquals(ConversationStep.MENU, conversation.step());
  }

  @Test
  void todayOnlyOffersHoursTheSchedulingRulesStillAccept() {
    say("1");
    say("1");
    say("1");

    String times = last(say("1"));

    // 08:00 now, at least 30 minutes ahead: 08:00 is gone, 09:00 is the first.
    assertTrue(times.contains("1. 09:00"), times);
  }

  @Test
  void aSlotTakenMeanwhileOffersTheOtherHours() {
    when(schedule.schedule(any())).thenThrow(new SlotFullyBookedError(NOW, 3));
    say("1");
    say("1");
    say("1");
    say("2");
    say("1");

    List<String> replies = say("1");

    assertEquals("Esse horário acabou de ser ocupado. Escolha outro:", replies.get(0));
    assertEquals(ConversationStep.CHOOSE_TIME, conversation.step());
  }

  @Test
  void registersANewCarOnTheWay() {
    Vehicle golf = new Vehicle(UUID.randomUUID(), ANA.id(), "XYZ1234", "Golf", null);
    when(createVehicle.create(any())).thenReturn(golf);
    say("1");

    assertEquals("Qual a placa do carro? (ex.: ABC1D23)", last(say("2")));
    assertTrue(last(say("placa?")).startsWith("Não reconheci a placa"));
    assertEquals("E qual o modelo do carro? (ex.: Onix)", last(say("xyz-1234")));
    when(vehicles.list(any())).thenReturn(List.of(ONIX, golf));
    String services = last(say("Golf"));

    verify(createVehicle).create(new CreateVehicle.Params(ANA.id(), "XYZ1234", "Golf", null));
    assertTrue(services.startsWith("Quais serviços?"), services);
  }

  @Test
  void listsAndCancelsAScheduledOrder() {
    ServiceOrder scheduled = order(ServiceOrderStatus.SCHEDULED, NOW.plus(Duration.ofDays(1)));
    ServiceOrder washing = order(ServiceOrderStatus.WASHING, NOW);
    when(orders.list(any())).thenReturn(List.of(scheduled, washing));

    String list = last(say("2"));
    assertTrue(list.contains("1. amanhã (sáb 21/11) às 08:00 — Onix (ABC1D23)"), list);
    assertTrue(list.contains("• hoje (sex 20/11) às 08:00 — Onix (ABC1D23)"), list);
    assertTrue(list.contains("(em lavagem)"), list);
    assertTrue(last(say("1")).startsWith("Cancelar amanhã"));

    String done = last(say("1"));

    verify(cancel)
        .cancel(new CancelServiceOrder.Params(scheduled.id(), ANA.id(), "CUSTOMER_GAVE_UP", null));
    assertTrue(done.startsWith("Agendamento cancelado"), done);
  }

  @Test
  void handsOverToTheStaffAndComesBackWithMenu() {
    say("3");
    assertEquals(ConversationStep.HUMAN, conversation.step());

    assertEquals(List.of(), say("quero falar sobre o preço"));
    assertTrue(last(say("menu")).contains("1. Agendar lavagem"));
  }

  @Test
  void theHandOverEndsAfterTwoHours() {
    say("3");
    now = NOW.plus(WhatsappAssistant.HUMAN_HANDOFF).plusSeconds(1);

    assertTrue(last(say("oi")).contains("1. Agendar lavagem"));
  }

  @Test
  void aStaleConversationStartsOver() {
    say("1");
    say("1");
    now = NOW.plus(WhatsappAssistant.IDLE_RESET).plusSeconds(1);

    // "1" now picks the first menu option, not the first service.
    assertTrue(last(say("1")).startsWith("Qual carro?"));
  }

  @Test
  void anAnswerThatIsNoOptionAsksAgain() {
    say("1");

    List<String> replies = say("o prata");

    assertTrue(replies.get(0).startsWith("Não entendi"));
    assertTrue(replies.get(1).startsWith("Qual carro?"));
    verify(schedule, never()).schedule(any());
  }

  @Test
  void theStaffNumberIsNotServed() {
    customer = new User(UUID.randomUUID(), "João", "92991234567", UserProfile.WASHER, NOW, true);

    assertTrue(say("1").get(0).contains("equipe"));
  }

  @Test
  void freeTextFillsInTheRequestAndGoesStraightToConfirmation() {
    assistant =
        new WhatsappAssistant(
            new WhatsappAssistant.Tools(
                prices,
                vehicles,
                createVehicle,
                availability,
                schedule,
                orders,
                cancel,
                createUser),
            interpreter);
    when(interpreter.interpret(any(), any()))
        .thenReturn(
            Optional.of(
                new MessageInterpreter.Interpretation(
                    MessageInterpreter.Intent.SCHEDULE,
                    ONIX.id(),
                    null,
                    List.of(1, 99),
                    TODAY.plusDays(1),
                    LocalTime.of(14, 0),
                    null)));

    String confirm = last(say("quero lavar o onix amanhã às 14h"));

    assertEquals(ConversationStep.CONFIRM, conversation.step());
    // The unknown service id 99 is dropped; only the catalog counts.
    assertTrue(confirm.contains("🧽 Lavagem Simples\n"), confirm);
    assertTrue(confirm.contains("amanhã (sáb 21/11) às 14:00"), confirm);
  }

  @Test
  void freeTextAskingForTheAfternoonOnlyOffersAfternoonHours() {
    assistant =
        new WhatsappAssistant(
            new WhatsappAssistant.Tools(
                prices,
                vehicles,
                createVehicle,
                availability,
                schedule,
                orders,
                cancel,
                createUser),
            interpreter);
    when(interpreter.interpret(any(), any()))
        .thenReturn(
            Optional.of(
                new MessageInterpreter.Interpretation(
                    MessageInterpreter.Intent.SCHEDULE,
                    ONIX.id(),
                    null,
                    List.of(1),
                    TODAY.plusDays(1),
                    null,
                    "AFTERNOON")));

    String times = last(say("amanhã à tarde, lavagem simples"));

    assertTrue(times.contains("1. 12:00"), times);
    assertTrue(!times.contains("09:00"), times);
  }

  private static ServiceOrder order(ServiceOrderStatus status, Instant scheduledAt) {
    return new ServiceOrder(
        UUID.randomUUID(),
        ONIX.id(),
        1,
        null,
        status,
        scheduledAt,
        NOW,
        NOW,
        null,
        null,
        null,
        new ServiceOrder.Details(
            "ABC1D23",
            "Onix",
            "Prata",
            "Ana Souza",
            "92991234567",
            "Lavagem Simples",
            null,
            new BigDecimal("40.00")),
        new ArrayList<>(List.of(1)));
  }
}
