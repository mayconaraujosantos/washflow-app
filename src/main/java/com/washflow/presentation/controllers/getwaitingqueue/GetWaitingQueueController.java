package com.washflow.presentation.controllers.getwaitingqueue;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.usecases.GetWaitingQueue;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;

/**
 * Delegates to {@code GetWaitingQueue} to return the waiting queue. Framework-agnostic — depends
 * only on HttpRequest/HttpResponse and the UseCase interface.
 */
public class GetWaitingQueueController implements Controller {

  private final GetWaitingQueue getWaitingQueue;

  public GetWaitingQueueController(GetWaitingQueue getWaitingQueue) {
    this.getWaitingQueue = getWaitingQueue;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    List<ServiceOrder> queue = getWaitingQueue.get();
    return new HttpResponse(200, queue);
  }
}
