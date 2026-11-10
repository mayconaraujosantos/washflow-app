package com.washflow.data.protocols.db.shared;

import com.washflow.domain.entities.pricing.ServicePrice;
import java.util.Optional;

public interface LoadServicePriceByIdRepository {

  Optional<ServicePrice> loadById(int id);
}
