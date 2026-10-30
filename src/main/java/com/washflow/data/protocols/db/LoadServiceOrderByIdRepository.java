package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;
import java.util.Optional;
import java.util.UUID;

public interface LoadServiceOrderByIdRepository {

  Optional<ServiceOrder> loadById(UUID id);
}
