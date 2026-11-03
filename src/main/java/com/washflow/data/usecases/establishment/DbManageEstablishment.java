package com.washflow.data.usecases.establishment;

import com.washflow.data.protocols.db.EstablishmentRepository;
import com.washflow.domain.entities.Establishment;
import com.washflow.domain.usecases.ManageEstablishment;

public class DbManageEstablishment implements ManageEstablishment {

  private final EstablishmentRepository repository;

  public DbManageEstablishment(EstablishmentRepository repository) {
    this.repository = repository;
  }

  @Override
  public Establishment get() {
    return repository.load();
  }

  @Override
  public Establishment update(Params params) {
    String name = EstablishmentRules.name(params.name());
    EstablishmentRules.hours(params.openingHour(), params.closingHour());
    EstablishmentRules.capacity(params.maxVehiclesPerHour());
    Establishment current = repository.load();
    return repository.update(
        new Establishment(
            name,
            params.openingHour(),
            params.closingHour(),
            params.openOnSunday(),
            params.maxVehiclesPerHour(),
            // Saving the settings means the place is set up: closes the setup wizard and
            // stops falling back to MAX_VEHICLES_PER_HOUR.
            current.configuredAt() != null ? current.configuredAt() : java.time.Instant.now()));
  }
}
