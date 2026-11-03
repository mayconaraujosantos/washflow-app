package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;
import java.time.Instant;
import java.util.List;

public interface LoadServiceOrdersScheduledBetweenRepository {

  /** Orders with {@code start <= data_agendamento < end}, ordered by {@code data_agendamento}. */
  List<ServiceOrder> loadScheduledBetween(Instant start, Instant end);
}
