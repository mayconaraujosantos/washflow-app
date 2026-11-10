package com.washflow.domain.usecases.schedule;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.time.LocalDate;
import java.util.List;

/**
 * Service orders whose {@code data_agendamento} falls between two shop-local (America/Manaus)
 * dates, in any status - backs the manager's calendar. The range is limited to 62 days.
 */
public interface ListAgenda {

  List<ServiceOrder> list(Params params);

  record Params(LocalDate from, LocalDate to) {}
}
