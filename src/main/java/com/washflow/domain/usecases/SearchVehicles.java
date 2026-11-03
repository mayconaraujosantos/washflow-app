package com.washflow.domain.usecases;

import com.washflow.domain.entities.VehicleLookup;
import java.util.List;

/** Finds vehicles by plate prefix, owner name or phone for the manager's entry form. */
public interface SearchVehicles {

  List<VehicleLookup> search(String query);
}
