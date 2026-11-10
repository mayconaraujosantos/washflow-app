package com.washflow.domain.usecases;

import com.washflow.domain.entities.EstablishmentListing;
import com.washflow.domain.entities.EstablishmentSummary;
import com.washflow.domain.entities.User;
import com.washflow.domain.errors.establishment.EstablishmentNotFoundError;
import com.washflow.domain.errors.establishment.EstablishmentSlugTakenError;
import com.washflow.domain.errors.user.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import java.util.List;
import java.util.UUID;

/** The platform administrator's work: sign in, create, list and suspend lava-jatos. */
public interface ManagePlatform {

  User signIn(String email, String password) throws InvalidCredentialsError;

  List<EstablishmentSummary> list();

  EstablishmentSummary create(NewEstablishment params)
      throws EstablishmentSlugTakenError, EmailAlreadyRegisteredError;

  EstablishmentSummary setActive(UUID establishmentId, boolean active)
      throws EstablishmentNotFoundError;

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
