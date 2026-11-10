package com.washflow.application.factories.usecases.establishment;

import com.washflow.application.AppConfig;
import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.domain.entities.establishment.Establishment;
import com.washflow.infra.db.jdbi.EstablishmentJdbiRepository;
import java.util.function.Supplier;
import org.jdbi.v3.core.Jdbi;

/**
 * Scheduling hours/capacity from the establishment settings. Until the owner configures them,
 * capacity keeps coming from {@code MAX_VEHICLES_PER_HOUR} so existing deployments behave the same
 * after the upgrade.
 */
public final class EstablishmentHours {

  private EstablishmentHours() {}

  public static Supplier<SchedulingRules.Hours> supplier(Jdbi jdbi, AppConfig appConfig) {
    EstablishmentJdbiRepository repository = new EstablishmentJdbiRepository(jdbi);
    return () -> {
      Establishment establishment = repository.load();
      return new SchedulingRules.Hours(
          establishment.openingHour(),
          establishment.closingHour(),
          establishment.openOnSunday(),
          establishment.isConfigured()
              ? establishment.maxVehiclesPerHour()
              : appConfig.maxVehiclesPerHour());
    };
  }
}
