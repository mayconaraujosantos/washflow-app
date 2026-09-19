package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServicePrice;
import java.util.Optional;

public interface LoadServicePriceByIdRepository {

  Optional<ServicePrice> loadById(int id);
}
