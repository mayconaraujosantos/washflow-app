package com.washflow.data.usecases.whatsapp;

import com.washflow.data.protocols.ai.MessageInterpreter;
import com.washflow.data.protocols.db.user.CreateUserRepository;
import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.domain.entities.pricing.ServicePrice;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.shared.ContactRules;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.entities.vehicle.Vehicle;
import com.washflow.domain.entities.whatsapp.Conversation;
import com.washflow.domain.entities.whatsapp.ConversationDraft;
import com.washflow.domain.entities.whatsapp.ConversationStep;
import com.washflow.domain.errors.common.DomainError;
import com.washflow.domain.errors.schedule.InsufficientLeadTimeError;
import com.washflow.domain.errors.schedule.OutsideBusinessHoursError;
import com.washflow.domain.errors.schedule.SlotFullyBookedError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotCancellableError;
import com.washflow.domain.errors.vehicle.InvalidPlateError;
import com.washflow.domain.errors.vehicle.VehiclePlateAlreadyRegisteredError;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lava-jato's WhatsApp assistant: one customer message in, the replies and the next state of
 * the conversation out. It schedules, lists and cancels through the same use cases as the
 * customer's screens, so every rule (hours, lead time, capacity per hour, plate ownership) holds
 * the same way; the customer always confirms before anything is booked or cancelled.
 *
 * <p>Numbered menus work on any phone. With a {@link MessageInterpreter}, free text ("quero lavar o
 * Onix amanhã à tarde") fills in what the customer said and the assistant asks only for the rest.
 */
public class WhatsappAssistant {

  static final Duration IDLE_RESET = Duration.ofMinutes(30);
  static final Duration HUMAN_HANDOFF = Duration.ofHours(2);
  private static final Duration MIN_LEAD_TIME = Duration.ofMinutes(30);
  private static final int DAYS_OFFERED = 5;
  private static final int DAYS_SEARCHED = 14;
  private static final int MAX_NAME = 60;
  private static final int NOON = 12;
  private static final Set<String> RESET_WORDS =
      Set.of("menu", "0", "inicio", "início", "voltar", "sair");
  private static final Set<String> GREETINGS =
      Set.of("oi", "olá", "ola", "bom dia", "boa tarde", "boa noite", "oie", "opa", "eai", "e aí");
  private static final Set<ServiceOrderStatus> OPEN_STATUSES =
      Set.of(
          ServiceOrderStatus.SCHEDULED,
          ServiceOrderStatus.WAITING_IN_YARD,
          ServiceOrderStatus.WASHING,
          ServiceOrderStatus.PAUSED,
          ServiceOrderStatus.READY);
  private static final Pattern NUMBER = Pattern.compile("\\d+");
  private static final Pattern LETTER = Pattern.compile("\\p{L}");
  private static final String CUSTOMER_GAVE_UP = "CUSTOMER_GAVE_UP";

  private final Tools tools;
  private final MessageInterpreter interpreter;

  /** The use cases the assistant drives, the same ones behind the customer's screens. */
  public record Tools(
      ListServicePrices servicePrices,
      ListVehiclesByCustomer vehicles,
      CreateVehicle createVehicle,
      ScheduleAvailability availability,
      ScheduleServiceOrder schedule,
      ListServiceOrdersByCustomer orders,
      CancelServiceOrder cancel,
      CreateUserRepository createUser) {}

  /**
   * Who is writing: {@code phone} is the app phone to create an account with (null for numbers
   * outside Brazil) and {@code customer} the account already found for it, if any.
   */
  public record Contact(String establishmentName, String phone, User customer) {}

  /** The replies to send, in order (possibly none), and the conversation to store. */
  public record Turn(List<String> messages, Conversation next, User customer) {}

  /** {@code interpreter} may be null: numbered menus only. */
  public WhatsappAssistant(Tools tools, MessageInterpreter interpreter) {
    this.tools = tools;
    this.interpreter = interpreter;
  }

  public Turn reply(Contact contact, Conversation current, String text, Instant now) {
    Conversation conversation = current == null ? Conversation.fresh(now) : current;
    if (isIdle(conversation, now)) {
      conversation = Conversation.fresh(now);
    }
    Chat chat = new Chat(contact, conversation.draft(), now);
    ConversationStep next = chat.handle(conversation.step(), text == null ? "" : text.trim());
    return new Turn(
        List.copyOf(chat.messages), new Conversation(next, chat.draft, now), chat.customer);
  }

  private static boolean isIdle(Conversation conversation, Instant now) {
    if (conversation.step() == ConversationStep.HUMAN) {
      Instant until = conversation.draft().getHumanUntil();
      return until == null || !until.isAfter(now);
    }
    return conversation.step() != ConversationStep.MENU
        && conversation.updatedAt().plus(IDLE_RESET).isBefore(now);
  }

  /** One incoming message: collects the replies while it walks the conversation. */
  private final class Chat {

    private final Contact contact;
    private final Instant now;
    private final LocalDate today;
    private final List<String> messages = new ArrayList<>();
    private ConversationDraft draft;
    private User customer;

    Chat(Contact contact, ConversationDraft draft, Instant now) {
      this.contact = contact;
      this.draft = draft;
      this.now = now;
      this.today = now.atZone(SchedulingRules.STORE_ZONE).toLocalDate();
      this.customer = contact.customer();
    }

    ConversationStep handle(ConversationStep step, String text) {
      if (customer == null) {
        return withoutAccount(step, text);
      }
      if (customer.profile() != UserProfile.CUSTOMER) {
        say("Este número está cadastrado na equipe do " + establishment() + ".");
        return ConversationStep.MENU;
      }
      if (!customer.active()) {
        say("Sua conta está desativada. Fale com o " + establishment() + ".");
        return ConversationStep.MENU;
      }
      if (RESET_WORDS.contains(normalized(text))) {
        draft = new ConversationDraft();
        return menu();
      }
      return switch (step) {
        case MENU, ASK_NAME -> fromMenu(text);
        case HUMAN -> ConversationStep.HUMAN;
        case CHOOSE_VEHICLE -> chooseVehicle(text);
        case ASK_PLATE -> askPlate(text);
        case ASK_MODEL -> askModel(text);
        case CHOOSE_SERVICES -> chooseServices(text);
        case CHOOSE_DAY -> chooseDay(text);
        case CHOOSE_TIME -> chooseTime(text);
        case CONFIRM -> confirm(text);
        case CHOOSE_ORDER -> chooseOrder(text);
        case CONFIRM_CANCEL -> confirmCancel(text);
      };
    }

    // --- first contact ---------------------------------------------------------------------

    private ConversationStep withoutAccount(ConversationStep step, String text) {
      if (contact.phone() == null) {
        say("Por enquanto o assistente atende só números do Brasil.");
        return ConversationStep.MENU;
      }
      if (step != ConversationStep.ASK_NAME) {
        say(
            "Olá! Sou o assistente do "
                + establishment()
                + " 🚗\nPara começar, como podemos te chamar?");
        return ConversationStep.ASK_NAME;
      }
      String name = personName(text);
      if (name == null) {
        say("Me diga seu nome, por favor (ex.: Ana).");
        return ConversationStep.ASK_NAME;
      }
      customer =
          tools
              .createUser()
              .create(
                  new User(
                      UUID.randomUUID(), name, contact.phone(), UserProfile.CUSTOMER, now, true),
                  null);
      say("Prazer, " + AssistantTexts.firstName(name) + "!");
      return menu();
    }

    // --- menu ------------------------------------------------------------------------------

    private ConversationStep menu() {
      say(
          "Olá, "
              + AssistantTexts.firstName(customer.name())
              + "! Sou o assistente do "
              + establishment()
              + " 🚗\n\n1. Agendar lavagem\n2. Meus agendamentos\n3. Falar com o lava-jato\n\n"
              + (interpreter == null
                  ? "Responda com o número da opção."
                  : "Responda com o número ou escreva o que precisa."));
      return ConversationStep.MENU;
    }

    private ConversationStep fromMenu(String text) {
      switch (text) {
        case "1":
          draft = new ConversationDraft();
          return advance();
        case "2":
          return myOrders();
        case "3":
          return human();
        default:
          if (!GREETINGS.contains(normalized(text))) {
            Optional<MessageInterpreter.Interpretation> understood = interpret(text);
            if (understood.isPresent()) {
              return act(understood.get(), true);
            }
          }
          return menu();
      }
    }

    private ConversationStep human() {
      draft = new ConversationDraft();
      draft.setHumanUntil(now.plus(HUMAN_HANDOFF));
      say(
          "Certo! Alguém do "
              + establishment()
              + " vai responder por aqui. Para voltar ao assistente, envie *menu*.");
      return ConversationStep.HUMAN;
    }

    // --- scheduling ------------------------------------------------------------------------

    /** Asks for the first thing still missing; confirms once everything is chosen. */
    private ConversationStep advance() {
      List<Vehicle> cars = cars();
      if (draft.getVehicleId() == null
          || cars.stream().noneMatch(car -> car.id().equals(draft.getVehicleId()))) {
        draft.setVehicleId(null);
        return cars.isEmpty() ? askForPlate() : offerVehicles(cars);
      }
      List<ServicePrice> catalog = catalog();
      if (draft.getServiceIds().isEmpty()
          || !catalog.stream().map(ServicePrice::id).toList().containsAll(draft.getServiceIds())) {
        draft.setServiceIds(List.of());
        return offerServices(catalog);
      }
      if (draft.getDay() == null) {
        return offerDays();
      }
      List<Instant> free = freeSlots(draft.getDay());
      if (free.isEmpty()) {
        say("Não há horários livres em " + dayLabel(draft.getDay()) + ".");
        draft.setDay(null);
        return offerDays();
      }
      if (draft.getSlot() == null || !free.contains(draft.getSlot())) {
        draft.setSlot(matchTime(free).orElse(null));
        if (draft.getSlot() == null) {
          return offerTimes(free);
        }
      }
      return confirmPrompt();
    }

    private Optional<Instant> matchTime(List<Instant> free) {
      LocalTime wanted = draft.getTime();
      if (wanted == null) {
        return Optional.empty();
      }
      draft.setTime(null);
      Optional<Instant> match =
          free.stream()
              .filter(
                  slot ->
                      slot.atZone(SchedulingRules.STORE_ZONE).toLocalTime().getHour()
                          == wanted.getHour())
              .findFirst();
      if (match.isEmpty()) {
        say(
            "Às "
                + String.format("%02d:%02d", wanted.getHour(), wanted.getMinute())
                + " não tem vaga.");
      }
      return match;
    }

    private ConversationStep offerVehicles(List<Vehicle> cars) {
      StringBuilder text = new StringBuilder("Qual carro?\n");
      List<UUID> options = new ArrayList<>();
      for (Vehicle car : cars) {
        options.add(car.id());
        text.append(options.size()).append(". ").append(AssistantTexts.vehicle(car)).append('\n');
      }
      text.append(options.size() + 1).append(". Outro carro");
      draft.setVehicleOptions(options);
      say(text.toString());
      return ConversationStep.CHOOSE_VEHICLE;
    }

    private ConversationStep chooseVehicle(String text) {
      List<UUID> options = draft.getVehicleOptions();
      Optional<Integer> choice = single(text, options.size() + 1);
      if (choice.isEmpty()) {
        return fallback(text, () -> offerVehicles(cars()));
      }
      if (choice.get() == options.size() + 1) {
        return askForPlate();
      }
      draft.setVehicleId(options.get(choice.get() - 1));
      return advance();
    }

    private ConversationStep askForPlate() {
      say("Qual a placa do carro? (ex.: ABC1D23)");
      return ConversationStep.ASK_PLATE;
    }

    private ConversationStep askPlate(String text) {
      String plate;
      try {
        plate = Vehicle.normalizePlate(text);
      } catch (InvalidPlateError e) {
        say("Não reconheci a placa. Envie no formato ABC1D23 ou ABC1234.");
        return ConversationStep.ASK_PLATE;
      }
      Optional<Vehicle> own = cars().stream().filter(car -> car.plate().equals(plate)).findFirst();
      if (own.isPresent()) {
        draft.setVehicleId(own.get().id());
        return advance();
      }
      draft.setPlate(plate);
      say("E qual o modelo do carro? (ex.: Onix)");
      return ConversationStep.ASK_MODEL;
    }

    private ConversationStep askModel(String text) {
      String model =
          text.length() > Vehicle.MAX_MODEL ? text.substring(0, Vehicle.MAX_MODEL) : text;
      if (model.isBlank() || draft.getPlate() == null) {
        return askForPlate();
      }
      try {
        Vehicle car =
            tools
                .createVehicle()
                .create(new CreateVehicle.Params(customer.id(), draft.getPlate(), model, null));
        draft.setVehicleId(car.id());
        draft.setPlate(null);
        return advance();
      } catch (VehiclePlateAlreadyRegisteredError e) {
        say(
            "A placa "
                + draft.getPlate()
                + " já está cadastrada em outra conta. Responda 3 no menu para falar com o "
                + establishment()
                + ".");
        draft = new ConversationDraft();
        return ConversationStep.MENU;
      }
    }

    private ConversationStep offerServices(List<ServicePrice> catalog) {
      if (catalog.isEmpty()) {
        say("O " + establishment() + " ainda não cadastrou serviços. Tente mais tarde.");
        draft = new ConversationDraft();
        return ConversationStep.MENU;
      }
      StringBuilder text =
          new StringBuilder("Quais serviços? Pode escolher mais de um (ex.: 1,3).\n");
      List<Integer> options = new ArrayList<>();
      for (ServicePrice service : catalog) {
        options.add(service.id());
        text.append(options.size())
            .append(". ")
            .append(service.name())
            .append(" — ")
            .append(AssistantTexts.money(service.price()))
            .append('\n');
      }
      draft.setServiceOptions(options);
      say(text.toString().trim());
      return ConversationStep.CHOOSE_SERVICES;
    }

    private ConversationStep chooseServices(String text) {
      List<Integer> options = draft.getServiceOptions();
      List<Integer> picked = numbers(text);
      if (picked.isEmpty()
          || picked.stream().anyMatch(n -> n < 1 || n > options.size())
          || new LinkedHashSet<>(picked).size() != picked.size()) {
        return fallback(text, () -> offerServices(catalog()));
      }
      draft.setServiceIds(picked.stream().map(n -> options.get(n - 1)).toList());
      return advance();
    }

    private ConversationStep offerDays() {
      List<LocalDate> days = new ArrayList<>();
      for (int ahead = 0; ahead < DAYS_SEARCHED && days.size() < DAYS_OFFERED; ahead++) {
        LocalDate day = today.plusDays(ahead);
        if (!freeSlots(day).isEmpty()) {
          days.add(day);
        }
      }
      if (days.isEmpty()) {
        say(
            "Não há horários livres nos próximos dias. Responda 3 no menu para falar com o "
                + establishment()
                + ".");
        draft = new ConversationDraft();
        return ConversationStep.MENU;
      }
      StringBuilder text = new StringBuilder("Qual dia?\n");
      for (int i = 0; i < days.size(); i++) {
        text.append(i + 1).append(". ").append(dayLabel(days.get(i))).append('\n');
      }
      draft.setDayOptions(days);
      say(text.toString().trim());
      return ConversationStep.CHOOSE_DAY;
    }

    private ConversationStep chooseDay(String text) {
      Optional<Integer> choice = single(text, draft.getDayOptions().size());
      if (choice.isEmpty()) {
        return fallback(text, this::offerDays);
      }
      draft.setDay(draft.getDayOptions().get(choice.get() - 1));
      draft.setSlot(null);
      return advance();
    }

    private ConversationStep offerTimes(List<Instant> free) {
      List<Instant> slots = free;
      if (draft.getPeriod() != null) {
        List<Instant> inPeriod = free.stream().filter(this::inPeriod).toList();
        if (!inPeriod.isEmpty()) {
          slots = inPeriod;
        }
      }
      StringBuilder text =
          new StringBuilder("Horários livres " + dayPhrase(draft.getDay()) + ":\n");
      for (int i = 0; i < slots.size(); i++) {
        text.append(i + 1).append(". ").append(AssistantTexts.hour(slots.get(i))).append('\n');
      }
      draft.setSlotOptions(slots);
      say(text.toString().trim());
      return ConversationStep.CHOOSE_TIME;
    }

    private ConversationStep chooseTime(String text) {
      Optional<Integer> choice = single(text, draft.getSlotOptions().size());
      if (choice.isEmpty()) {
        return fallback(text, this::advance);
      }
      draft.setSlot(draft.getSlotOptions().get(choice.get() - 1));
      return advance();
    }

    private ConversationStep confirmPrompt() {
      Vehicle car =
          cars().stream()
              .filter(c -> c.id().equals(draft.getVehicleId()))
              .findFirst()
              .orElseThrow();
      List<ServicePrice> chosen = chosenServices();
      BigDecimal total =
          chosen.stream().map(ServicePrice::price).reduce(BigDecimal.ZERO, BigDecimal::add);
      say(
          "Confirma o agendamento?\n🚗 "
              + AssistantTexts.vehicle(car)
              + "\n🧽 "
              + String.join(" + ", chosen.stream().map(ServicePrice::name).toList())
              + "\n📅 "
              + AssistantTexts.when(draft.getSlot(), today)
              + "\n💰 "
              + AssistantTexts.money(total)
              + "\n\n1. Confirmar\n2. Recomeçar");
      return ConversationStep.CONFIRM;
    }

    private ConversationStep confirm(String text) {
      if ("1".equals(text)
          || normalized(text).startsWith("sim")
          || "confirmar".equals(normalized(text))) {
        return book();
      }
      if ("2".equals(text)) {
        draft = new ConversationDraft();
        return advance();
      }
      return fallback(text, this::confirmPrompt);
    }

    private ConversationStep book() {
      List<Integer> services = draft.getServiceIds();
      Instant slot = draft.getSlot();
      try {
        tools
            .schedule()
            .schedule(
                new ScheduleServiceOrder.Params(
                    draft.getVehicleId(), services.get(0), services, slot));
      } catch (SlotFullyBookedError | InsufficientLeadTimeError | OutsideBusinessHoursError e) {
        say("Esse horário acabou de ser ocupado. Escolha outro:");
        draft.setSlot(null);
        return advance();
      } catch (DomainError e) {
        say(e.getMessage());
        draft = new ConversationDraft();
        return ConversationStep.MENU;
      }
      say(
          "Agendado ✅ Te esperamos "
              + AssistantTexts.when(slot, today)
              + ". Uma hora antes mandamos um lembrete por aqui.");
      draft = new ConversationDraft();
      return ConversationStep.MENU;
    }

    // --- my orders and cancelling ----------------------------------------------------------

    private ConversationStep myOrders() {
      draft = new ConversationDraft();
      List<ServiceOrder> open =
          tools.orders().list(new ListServiceOrdersByCustomer.Params(customer.id())).stream()
              .filter(order -> OPEN_STATUSES.contains(order.status()))
              .sorted(Comparator.comparing(ServiceOrder::scheduledAt))
              .toList();
      if (open.isEmpty()) {
        say("Você não tem agendamentos em aberto. Envie 1 para agendar.");
        return ConversationStep.MENU;
      }
      StringBuilder text = new StringBuilder("Seus atendimentos:\n");
      List<UUID> cancellable = new ArrayList<>();
      for (ServiceOrder order : open) {
        if (order.status() == ServiceOrderStatus.SCHEDULED) {
          cancellable.add(order.id());
          text.append(cancellable.size()).append(". ");
        } else {
          text.append("• ");
        }
        text.append(orderLine(order)).append('\n');
      }
      if (cancellable.isEmpty()) {
        say(text.toString().trim());
        return ConversationStep.MENU;
      }
      draft.setOrderOptions(cancellable);
      say(
          text
              + "\nPara cancelar um agendamento, responda com o número dele. Para voltar, envie"
              + " *menu*.");
      return ConversationStep.CHOOSE_ORDER;
    }

    private ConversationStep chooseOrder(String text) {
      Optional<Integer> choice = single(text, draft.getOrderOptions().size());
      if (choice.isEmpty()) {
        return fallback(text, this::myOrders);
      }
      UUID order = draft.getOrderOptions().get(choice.get() - 1);
      draft.setOrderToCancel(order);
      String line = ownOrder(order).map(this::orderLine).orElse("este agendamento");
      say("Cancelar " + line + "?\n1. Sim, cancelar\n2. Não");
      return ConversationStep.CONFIRM_CANCEL;
    }

    private ConversationStep confirmCancel(String text) {
      UUID order = draft.getOrderToCancel();
      if (("1".equals(text) || normalized(text).startsWith("sim")) && order != null) {
        draft = new ConversationDraft();
        if (ownOrder(order).isEmpty()) {
          return myOrders();
        }
        try {
          tools
              .cancel()
              .cancel(new CancelServiceOrder.Params(order, customer.id(), CUSTOMER_GAVE_UP, null));
          say("Agendamento cancelado. Se quiser marcar outro horário, envie 1.");
        } catch (ServiceOrderNotCancellableError e) {
          say(
              "Esse atendimento não pode mais ser cancelado por aqui. Fale com o "
                  + establishment()
                  + ".");
        }
        return ConversationStep.MENU;
      }
      if ("2".equals(text) || normalized(text).startsWith("n")) {
        draft = new ConversationDraft();
        return menu();
      }
      return fallback(text, () -> chooseOrderAgain(order));
    }

    private ConversationStep chooseOrderAgain(UUID order) {
      say("Responda 1 para cancelar ou 2 para manter.");
      draft.setOrderToCancel(order);
      return ConversationStep.CONFIRM_CANCEL;
    }

    private Optional<ServiceOrder> ownOrder(UUID orderId) {
      return tools.orders().list(new ListServiceOrdersByCustomer.Params(customer.id())).stream()
          .filter(order -> order.id().equals(orderId))
          .findFirst();
    }

    private String orderLine(ServiceOrder order) {
      ServiceOrder.Details details = order.details();
      String vehicle = details == null ? "Carro" : AssistantTexts.vehicle(details);
      String service =
          details == null || details.serviceName() == null ? "" : " — " + details.serviceName();
      return AssistantTexts.when(order.scheduledAt(), today)
          + " — "
          + vehicle
          + service
          + (order.status() == ServiceOrderStatus.SCHEDULED
              ? ""
              : " (" + AssistantTexts.status(order.status()) + ")");
    }

    // --- free text ---------------------------------------------------------------------------

    /**
     * An answer that isn't one of the numbers offered: free text when the interpreter understands
     * it, otherwise the same question again.
     */
    private ConversationStep fallback(String text, Supplier<ConversationStep> askAgain) {
      Optional<MessageInterpreter.Interpretation> understood = interpret(text);
      if (understood.isPresent() && understood.get().intent() != MessageInterpreter.Intent.OTHER) {
        return act(understood.get(), false);
      }
      say("Não entendi. Responda com o número da opção, ou envie *menu* para recomeçar.");
      return askAgain.get();
    }

    private Optional<MessageInterpreter.Interpretation> interpret(String text) {
      if (interpreter == null || !LETTER.matcher(text).find()) {
        return Optional.empty();
      }
      return interpreter.interpret(
          text,
          new MessageInterpreter.Context(
              today,
              catalog().stream()
                  .map(s -> new MessageInterpreter.ServiceOption(s.id(), s.name()))
                  .toList(),
              cars().stream()
                  .map(
                      car ->
                          new MessageInterpreter.VehicleOption(
                              car.id(), AssistantTexts.vehicle(car)))
                  .toList()));
    }

    /** {@code fresh}: a new request from the menu, rather than a correction mid-scheduling. */
    private ConversationStep act(MessageInterpreter.Interpretation understood, boolean fresh) {
      return switch (understood.intent()) {
        case SCHEDULE -> {
          if (fresh) {
            draft = new ConversationDraft();
          }
          apply(understood);
          yield advance();
        }
        case MY_ORDERS, CANCEL -> myOrders();
        case HUMAN -> human();
        case OTHER -> {
          say("Não entendi muito bem.");
          yield menu();
        }
      };
    }

    /** Keeps only what points at this customer's cars, the catalog and real dates. */
    private void apply(MessageInterpreter.Interpretation understood) {
      List<Vehicle> cars = cars();
      if (understood.vehicleId() != null
          && cars.stream().anyMatch(car -> car.id().equals(understood.vehicleId()))) {
        draft.setVehicleId(understood.vehicleId());
      } else if (understood.plate() != null) {
        String plate = understood.plate().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        cars.stream()
            .filter(car -> car.plate().equals(plate))
            .findFirst()
            .ifPresent(car -> draft.setVehicleId(car.id()));
      }
      List<Integer> catalogIds = catalog().stream().map(ServicePrice::id).toList();
      List<Integer> services =
          understood.serviceIds().stream().filter(catalogIds::contains).distinct().toList();
      if (!services.isEmpty()) {
        draft.setServiceIds(services);
      }
      LocalDate date = understood.date();
      if (date != null && !date.isBefore(today) && date.isBefore(today.plusDays(DAYS_SEARCHED))) {
        draft.setDay(date);
        draft.setSlot(null);
      }
      if (understood.time() != null) {
        draft.setTime(understood.time());
        draft.setSlot(null);
      }
      if ("MORNING".equals(understood.period()) || "AFTERNOON".equals(understood.period())) {
        draft.setPeriod(understood.period());
      }
    }

    // --- helpers ---------------------------------------------------------------------------

    private List<Vehicle> cars() {
      return tools.vehicles().list(new ListVehiclesByCustomer.Params(customer.id()));
    }

    private List<ServicePrice> catalog() {
      return tools.servicePrices().list();
    }

    private List<ServicePrice> chosenServices() {
      List<ServicePrice> catalog = catalog();
      return draft.getServiceIds().stream()
          .flatMap(id -> catalog.stream().filter(s -> s.id() == id).limit(1))
          .toList();
    }

    /** Free hours of a shop-local day the scheduling rules will still accept now. */
    private List<Instant> freeSlots(LocalDate day) {
      ScheduleAvailability.Day availability = tools.availability().forDate(day);
      if (!availability.open()) {
        return List.of();
      }
      Instant earliest = now.plus(MIN_LEAD_TIME);
      return availability.slots().stream()
          .filter(ScheduleAvailability.Slot::available)
          .map(ScheduleAvailability.Slot::startsAt)
          .filter(start -> start.isAfter(earliest))
          .toList();
    }

    private boolean inPeriod(Instant slot) {
      int hour = slot.atZone(SchedulingRules.STORE_ZONE).getHour();
      return "MORNING".equals(draft.getPeriod()) ? hour < NOON : hour >= NOON;
    }

    private String dayLabel(LocalDate day) {
      return AssistantTexts.day(day, today);
    }

    private String dayPhrase(LocalDate day) {
      String label = dayLabel(day);
      if (day.equals(today) || day.equals(today.plusDays(1))) {
        return label.substring(0, label.indexOf(' ')).toLowerCase(Locale.ROOT);
      }
      return "em " + label;
    }

    private String establishment() {
      return contact.establishmentName();
    }

    private void say(String message) {
      messages.add(message);
    }
  }

  private static String normalized(String text) {
    return text.toLowerCase(Locale.ROOT).replaceAll("[!?.,]", "").trim();
  }

  private static Optional<Integer> single(String text, int max) {
    List<Integer> picked = numbers(text);
    if (picked.size() != 1 || picked.get(0) < 1 || picked.get(0) > max) {
      return Optional.empty();
    }
    return Optional.of(picked.get(0));
  }

  private static List<Integer> numbers(String text) {
    // Only short answers count as choices: "2" or "1, 3" - not a plate or a phone number.
    if (text.length() > 20) {
      return List.of();
    }
    List<Integer> found = new ArrayList<>();
    Matcher matcher = NUMBER.matcher(text);
    while (matcher.find()) {
      String digits = matcher.group();
      if (digits.length() > 2) {
        return List.of();
      }
      found.add(Integer.parseInt(digits));
    }
    return found;
  }

  private static String personName(String text) {
    if (text.length() < 2 || text.length() > MAX_NAME || !LETTER.matcher(text).find()) {
      return null;
    }
    return ContactRules.limited(text, MAX_NAME, "Nome");
  }
}
