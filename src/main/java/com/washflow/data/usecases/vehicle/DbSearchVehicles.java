package com.washflow.data.usecases.vehicle;

import com.washflow.data.protocols.db.vehicle.SearchVehiclesRepository;
import com.washflow.domain.entities.vehicle.VehicleLookup;
import com.washflow.domain.usecases.vehicle.SearchVehicles;
import java.util.List;
import java.util.Locale;

/** Matches plate prefix, owner name (contains) or phone digits (contains); at most 10 results. */
public class DbSearchVehicles implements SearchVehicles {

  private static final int MIN_QUERY_LENGTH = 2;
  private static final int MIN_PHONE_DIGITS = 3;
  private static final int LIMIT = 10;

  private final SearchVehiclesRepository searchVehiclesRepository;

  public DbSearchVehicles(SearchVehiclesRepository searchVehiclesRepository) {
    this.searchVehiclesRepository = searchVehiclesRepository;
  }

  @Override
  public List<VehicleLookup> search(String query) {
    String trimmed = query == null ? "" : query.trim();
    if (trimmed.length() < MIN_QUERY_LENGTH) {
      return List.of();
    }
    String platePrefix = trimmed.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    String text = trimmed.toLowerCase(Locale.ROOT);
    String digits = trimmed.replaceAll("\\D", "");
    // A one- or two-digit fragment would match most phone numbers.
    if (digits.length() < MIN_PHONE_DIGITS) {
      digits = "";
    }
    return searchVehiclesRepository.search(platePrefix, text, digits, LIMIT);
  }
}
