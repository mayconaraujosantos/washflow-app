package com.washflow.domain.usecases;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import java.util.UUID;

/**
 * Backs RF-06 from {@code docs/feature_espec_visao_do_cliente.md} - the client's status-tracking
 * panel polling a specific service order it already knows the id of (e.g. from the response of
 * {@code ScheduleServiceOrder}). Implemented by {@code DbGetServiceOrderById} in the data layer.
 */
public interface GetServiceOrderById {

  ServiceOrder get(Params params) throws ServiceOrderNotFoundError;

  record Params(UUID serviceOrderId) {}
}
