package com.washflow.domain.usecases.establishment;

import com.washflow.domain.entities.establishment.EstablishmentListing;
import com.washflow.domain.entities.establishment.EstablishmentSummary;
import com.washflow.domain.entities.user.User;
import java.util.List;
import java.util.UUID;

/** The platform administrator's work: sign in, create, list and suspend lava-jatos. */
public interface ManagePlatform {

  User signIn(String email, String password);

  List<EstablishmentSummary> list();

  EstablishmentSummary create(NewEstablishment params);

  EstablishmentSummary setActive(UUID establishmentId, boolean active);

  /**
   * Sets a new password for the lava-jato's manager (the one shown on the admin panel), for a
   * manager who lost theirs. The administrator hands it over; the manager signs in with it.
   */
  EstablishmentSummary resetManagerPassword(UUID establishmentId, String newPassword);

  /** Public: active lava-jatos whose name or address matches, for the customer picker. */
  List<EstablishmentListing> search(String query);

  record NewEstablishment(
      String name,
      String slug,
      int openingHour,
      int closingHour,
      boolean openOnSunday,
      int maxVehiclesPerHour,
      String managerName,
      String managerEmail,
      String managerPhone,
      String managerPassword) {}
}
