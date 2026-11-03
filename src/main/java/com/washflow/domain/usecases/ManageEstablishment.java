package com.washflow.domain.usecases;

import com.washflow.domain.entities.Establishment;

/** Read (public: name and hours) and update (manager) the establishment parameters. */
public interface ManageEstablishment {

  Establishment get();

  Establishment update(Params params);

  record Params(
      String name,
      int openingHour,
      int closingHour,
      boolean openOnSunday,
      int maxVehiclesPerHour) {}
}
