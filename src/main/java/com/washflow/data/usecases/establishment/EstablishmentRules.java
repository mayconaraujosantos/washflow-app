package com.washflow.data.usecases.establishment;

import com.washflow.domain.entities.shared.ContactRules;

/** Validation shared by the setup wizard and the settings screen. */
public final class EstablishmentRules {

  private static final int MAX_CAPACITY = 50;

  private EstablishmentRules() {}

  public static String name(String raw) {
    String name = ContactRules.limited(raw, 100, "Nome do estabelecimento");
    if (name == null || name.length() < 2) {
      throw new IllegalArgumentException("Informe o nome do estabelecimento");
    }
    return name;
  }

  public static void hours(int openingHour, int closingHour) {
    if (openingHour < 0 || closingHour > 24 || openingHour >= closingHour) {
      throw new IllegalArgumentException(
          "Horário inválido: a abertura deve ser antes do fechamento (0 a 24h)");
    }
  }

  public static void capacity(int maxVehiclesPerHour) {
    if (maxVehiclesPerHour < 1 || maxVehiclesPerHour > MAX_CAPACITY) {
      throw new IllegalArgumentException(
          "Capacidade deve ficar entre 1 e " + MAX_CAPACITY + " veículos por hora");
    }
  }
}
