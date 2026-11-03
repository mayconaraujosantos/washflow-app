package com.washflow.data.usecases.registervehicleentry;

import com.washflow.data.protocols.db.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.data.protocols.db.CreateServiceOrderRepository;
import com.washflow.data.protocols.db.CreateVehicleRepository;
import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.LoadVehicleByPlateRepository;
import com.washflow.data.usecases.scheduleserviceorder.SchedulingRules;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.InsufficientLeadTimeError;
import com.washflow.domain.errors.InvalidPlateError;
import com.washflow.domain.errors.OutsideBusinessHoursError;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.errors.SlotFullyBookedError;
import com.washflow.domain.usecases.RegisterVehicleEntry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Only talks to {@code data.protocols.db} ports. A walk-in vehicle is not turned into a {@code
 * usuarios} account: that would block the real customer from signing up with that phone later.
 * Instead the vehicle keeps a contact name/phone, and is linked to an existing customer account
 * when the phone already belongs to one.
 */
public class DbRegisterVehicleEntry implements RegisterVehicleEntry {

  // Old format ABC1234 and Mercosul ABC1D23.
  private static final Pattern PLATE = Pattern.compile("^[A-Z]{3}\\d[A-Z0-9]\\d{2}$");
  private static final Pattern PHONE = Pattern.compile("^\\d{10,11}$");
  private static final int MAX_NOTES = 500;
  private static final int MAX_NAME = 100;
  private static final int MAX_MODEL = 50;
  private static final int MAX_COLOR = 30;

  private final LoadVehicleByPlateRepository loadVehicleByPlateRepository;
  private final CreateVehicleRepository createVehicleRepository;
  private final LoadUserByPhoneRepository loadUserByPhoneRepository;
  private final LoadServicePriceByIdRepository loadServicePriceByIdRepository;
  private final CreateServiceOrderRepository createServiceOrderRepository;
  private final LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;
  private final SchedulingRules rules;
  private final Clock clock;

  @SuppressWarnings("java:S107") // one port per collaborator, like the other Db* use cases
  public DbRegisterVehicleEntry(
      LoadVehicleByPlateRepository loadVehicleByPlateRepository,
      CreateVehicleRepository createVehicleRepository,
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      LoadServicePriceByIdRepository loadServicePriceByIdRepository,
      CountServiceOrdersScheduledInRangeRepository countServiceOrdersScheduledInRangeRepository,
      CreateServiceOrderRepository createServiceOrderRepository,
      LoadServiceOrderByIdRepository loadServiceOrderByIdRepository,
      Clock clock,
      int maxVehiclesPerHour) {
    this.loadVehicleByPlateRepository = loadVehicleByPlateRepository;
    this.createVehicleRepository = createVehicleRepository;
    this.loadUserByPhoneRepository = loadUserByPhoneRepository;
    this.loadServicePriceByIdRepository = loadServicePriceByIdRepository;
    this.createServiceOrderRepository = createServiceOrderRepository;
    this.loadServiceOrderByIdRepository = loadServiceOrderByIdRepository;
    this.rules =
        new SchedulingRules(countServiceOrdersScheduledInRangeRepository, maxVehiclesPerHour);
    this.clock = clock;
  }

  @Override
  public ServiceOrder register(Params params)
      throws InvalidPlateError,
          ServicePriceNotFoundError,
          OutsideBusinessHoursError,
          InsufficientLeadTimeError,
          SlotFullyBookedError {
    Instant now = Instant.now(clock);
    String plate = normalizePlate(params.plate());
    String phone = normalizePhone(params.customerPhone());
    String notes = limited(params.notes(), MAX_NOTES, "Observações");
    checkExpectedDelivery(params, now);

    loadServicePriceByIdRepository
        .loadById(params.servicePriceId())
        .orElseThrow(() -> new ServicePriceNotFoundError(params.servicePriceId()));

    boolean scheduled = params.scheduledAt() != null;
    if (scheduled) {
      rules.checkBusinessHours(params.scheduledAt());
      rules.checkLeadTime(params.scheduledAt(), now, Duration.ZERO);
      rules.checkCapacity(params.scheduledAt());
    }

    Vehicle vehicle =
        loadVehicleByPlateRepository
            .loadByPlate(plate)
            .orElseGet(() -> createVehicle(plate, phone, params));

    ServiceOrder created =
        createServiceOrderRepository.create(
            new ServiceOrder(
                UUID.randomUUID(),
                vehicle.id(),
                params.servicePriceId(),
                null,
                scheduled ? ServiceOrderStatus.SCHEDULED : ServiceOrderStatus.WAITING_IN_YARD,
                scheduled ? params.scheduledAt() : now,
                now,
                now,
                null,
                params.expectedDeliveryAt(),
                notes,
                null));

    return loadServiceOrderByIdRepository.loadById(created.id()).orElse(created);
  }

  private Vehicle createVehicle(String plate, String phone, Params params) {
    UUID customerId =
        phone == null
            ? null
            : loadUserByPhoneRepository
                .loadByPhone(phone)
                .filter(user -> user.profile() == UserProfile.CUSTOMER && user.active())
                .map(User::id)
                .orElse(null);
    boolean walkIn = customerId == null;
    return createVehicleRepository.create(
        new Vehicle(
            UUID.randomUUID(),
            customerId,
            plate,
            limited(params.vehicleModel(), MAX_MODEL, "Modelo"),
            limited(params.vehicleColor(), MAX_COLOR, "Cor"),
            walkIn ? limited(params.customerName(), MAX_NAME, "Nome") : null,
            walkIn ? phone : null));
  }

  static String normalizePlate(String raw) throws InvalidPlateError {
    String plate = raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    if (!PLATE.matcher(plate).matches()) {
      throw new InvalidPlateError(raw == null ? "" : raw);
    }
    return plate;
  }

  private static String normalizePhone(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String digits = raw.replaceAll("\\D", "");
    if (digits.startsWith("55") && digits.length() > 11) {
      digits = digits.substring(2);
    }
    if (!PHONE.matcher(digits).matches()) {
      throw new IllegalArgumentException("Telefone deve ter DDD + número (10 ou 11 dígitos)");
    }
    return digits;
  }

  private static void checkExpectedDelivery(Params params, Instant now) {
    Instant expected = params.expectedDeliveryAt();
    if (expected == null) {
      return;
    }
    Instant start = params.scheduledAt() == null ? now : params.scheduledAt();
    if (!expected.isAfter(start)) {
      throw new IllegalArgumentException("A previsão de entrega deve ser depois da entrada");
    }
  }

  private static String limited(String value, int max, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() > max) {
      throw new IllegalArgumentException(field + " deve ter no máximo " + max + " caracteres");
    }
    return trimmed;
  }
}
