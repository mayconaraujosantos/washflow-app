package com.washflow.domain.usecases;

import com.washflow.domain.entities.User;
import com.washflow.domain.errors.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.EstablishmentAlreadyConfiguredError;
import com.washflow.domain.errors.InvalidSetupCodeError;
import com.washflow.domain.errors.PhoneAlreadyRegisteredError;

/**
 * First-run wizard: names the car wash, sets its hours and yard capacity, and creates the owner
 * manager account (e-mail + password). Allowed only once; in production it also requires the {@code
 * SETUP_CODE} configured on the server, proving the caller owns the deployment.
 */
public interface SetupEstablishment {

  Status status();

  User setup(Params params)
      throws EstablishmentAlreadyConfiguredError,
          InvalidSetupCodeError,
          EmailAlreadyRegisteredError,
          PhoneAlreadyRegisteredError;

  record Status(boolean configured, boolean setupCodeRequired, String establishmentName) {}

  record Params(
      String setupCode,
      String establishmentName,
      int openingHour,
      int closingHour,
      boolean openOnSunday,
      int maxVehiclesPerHour,
      String ownerName,
      String ownerEmail,
      String ownerPhone,
      String ownerPassword) {}
}
