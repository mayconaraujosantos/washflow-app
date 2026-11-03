package com.washflow.data.protocols.db;

import com.washflow.domain.entities.VehicleLookup;
import java.util.List;

public interface SearchVehiclesRepository {

  /**
   * {@code platePrefix} is upper-case alphanumerics, {@code text} lower-case, {@code digits} only
   * digits; an empty value never matches.
   */
  List<VehicleLookup> search(String platePrefix, String text, String digits, int limit);
}
