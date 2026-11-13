package com.washflow.data.usecases.whatsapp;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.usecases.serviceorder.CompleteServiceOrder;

/**
 * Decorates {@link CompleteServiceOrder}: once the car is {@code PRONTO}, the customer gets a
 * WhatsApp notice. The wash itself never depends on the notice (see {@link WhatsappNotices}).
 */
public class NotifyingCompleteServiceOrder implements CompleteServiceOrder {

  private final CompleteServiceOrder completeServiceOrder;
  private final WhatsappNotices notices;

  public NotifyingCompleteServiceOrder(
      CompleteServiceOrder completeServiceOrder, WhatsappNotices notices) {
    this.completeServiceOrder = completeServiceOrder;
    this.notices = notices;
  }

  @Override
  public ServiceOrder complete(Params params) {
    ServiceOrder completed = completeServiceOrder.complete(params);
    notices.ready(completed.id());
    return completed;
  }
}
